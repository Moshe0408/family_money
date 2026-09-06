-- =============================================================================
--  הכסף שלנו — Supabase schema for app update distribution
--
--  Run this once in:  Supabase Dashboard -> SQL Editor -> New query -> Run
--
--  What it creates:
--    * app_releases  — one row per published APK version
--    * storage bucket "apk" — where the APK files live
--    * RLS policies  — anyone can READ (so the app can check for updates),
--                      only a signed-in admin can WRITE.
-- =============================================================================


-- ---------------------------------------------------------------- the table

create table if not exists public.app_releases (
    id             uuid primary key default gen_random_uuid(),

    -- Must increase with every release. The app compares this against its own
    -- BuildConfig.VERSION_CODE to decide whether an update exists.
    version_code   integer     not null unique,

    -- Human readable, e.g. "1.1.0". Shown in the update dialog.
    version_name   text        not null,

    -- Public URL of the APK in Supabase Storage.
    apk_url        text        not null,

    -- Bytes. Shown so the user knows what they are about to download.
    file_size      bigint      not null default 0,

    -- Lowercase hex SHA-256 of the APK. The app verifies this after download
    -- and refuses to install on mismatch.
    sha256         text,

    -- Markdown-free plain text, shown as "what's new".
    release_notes  text        not null default '',

    -- When true the app blocks use until the update is installed.
    mandatory      boolean     not null default false,

    -- Unpublished rows are invisible to the app; lets you stage a release.
    published      boolean     not null default true,

    min_sdk        integer     not null default 26,
    created_at     timestamptz not null default now()
);

comment on table public.app_releases is
    'Published APK versions of the Family Money app. Read by the in-app updater.';

create index if not exists app_releases_lookup_idx
    on public.app_releases (published, version_code desc);


-- ------------------------------------------------------------ row security

alter table public.app_releases enable row level security;

-- The app checks for updates without signing in, so reads are open. Only
-- published rows are visible, which keeps drafts private.
drop policy if exists "anyone can read published releases" on public.app_releases;
create policy "anyone can read published releases"
    on public.app_releases
    for select
    using (published = true);

-- Writes require a signed-in user. Create that user in
-- Dashboard -> Authentication -> Users -> Add user.
drop policy if exists "admins can insert releases" on public.app_releases;
create policy "admins can insert releases"
    on public.app_releases
    for insert
    to authenticated
    with check (true);

drop policy if exists "admins can update releases" on public.app_releases;
create policy "admins can update releases"
    on public.app_releases
    for update
    to authenticated
    using (true)
    with check (true);

drop policy if exists "admins can delete releases" on public.app_releases;
create policy "admins can delete releases"
    on public.app_releases
    for delete
    to authenticated
    using (true);

-- Signed-in admins also need to see drafts.
drop policy if exists "admins can read every release" on public.app_releases;
create policy "admins can read every release"
    on public.app_releases
    for select
    to authenticated
    using (true);


-- --------------------------------------------------------------- storage

-- Public bucket: the APK download URL has to work without a token, because
-- Android's DownloadManager fetches it outside the app's HTTP stack.
insert into storage.buckets (id, name, public)
values ('apk', 'apk', true)
on conflict (id) do update set public = true;

drop policy if exists "anyone can download apks" on storage.objects;
create policy "anyone can download apks"
    on storage.objects
    for select
    using (bucket_id = 'apk');

drop policy if exists "admins can upload apks" on storage.objects;
create policy "admins can upload apks"
    on storage.objects
    for insert
    to authenticated
    with check (bucket_id = 'apk');

drop policy if exists "admins can replace apks" on storage.objects;
create policy "admins can replace apks"
    on storage.objects
    for update
    to authenticated
    using (bucket_id = 'apk');

drop policy if exists "admins can remove apks" on storage.objects;
create policy "admins can remove apks"
    on storage.objects
    for delete
    to authenticated
    using (bucket_id = 'apk');


-- ------------------------------------------------- convenience for the app

-- Single-row view of the newest published release. The updater hits this
-- instead of sorting client-side.
create or replace view public.latest_release
with (security_invoker = true)
as
    select version_code, version_name, apk_url, file_size,
           sha256, release_notes, mandatory, min_sdk, created_at
    from public.app_releases
    where published = true
    order by version_code desc
    limit 1;

comment on view public.latest_release is
    'Newest published release. Polled by the in-app update checker.';
