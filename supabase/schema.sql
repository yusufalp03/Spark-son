-- ============================================================
-- Spark - Supabase production schema
-- Supabase Dashboard > SQL Editor'de bu dosyanın tamamını çalıştırın.
-- Güvenlik modeli: tüm tablolarda RLS açık; istemci yalnızca kendi
-- verisini yazabilir, keşif/eşleşme mantığı SECURITY DEFINER RPC'lerde.
--
-- DİKKAT: Aşağıdaki drop bloğu, uygulamaya ait eski/deneme tabloları
-- (farklı kolon yapısıyla önceden oluşturulmuş olabilir) tamamen siler.
-- Bu script temiz kurulum içindir; korumak istediğiniz gerçek veri varsa
-- önce yedekleyin.
-- ============================================================

drop table if exists public.messages cascade;
drop table if exists public.likes cascade;
drop table if exists public.matches cascade;
drop table if exists public.feedback cascade;
drop table if exists public.profiles cascade;
drop table if exists public.user_music_tastes cascade; -- eski sürümden kalma
drop table if exists public.chat_messages cascade;     -- eski sürümden kalma
drop table if exists public.user_feedback cascade;     -- eski sürümden kalma

drop function if exists public.get_discover_profiles(int);
drop function if exists public.handle_swipe(uuid, boolean);
drop function if exists public.get_my_matches();

-- ---------- PROFILES ----------
create table if not exists public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  name text not null default '',
  age int check (age is null or age >= 18),
  bio text not null default '',
  avatar_url text not null default '',
  favorite_genre text not null default '',
  top_artists text not null default '',
  top_tracks text not null default '',
  signature_song_id text not null default '',
  signature_song_title text not null default '',
  signature_song_artist text not null default '',
  signature_song_trim_start real not null default 15,
  signature_song_trim_end real not null default 45,
  updated_at timestamptz not null default now()
);

alter table public.profiles enable row level security;

create policy "profiles_select_authenticated"
  on public.profiles for select
  to authenticated
  using (true);

create policy "profiles_insert_own"
  on public.profiles for insert
  to authenticated
  with check (id = auth.uid());

create policy "profiles_update_own"
  on public.profiles for update
  to authenticated
  using (id = auth.uid())
  with check (id = auth.uid());

-- ---------- LIKES (swipes) ----------
create table if not exists public.likes (
  liker_id uuid not null references public.profiles (id) on delete cascade,
  liked_id uuid not null references public.profiles (id) on delete cascade,
  is_like boolean not null,
  created_at timestamptz not null default now(),
  primary key (liker_id, liked_id),
  check (liker_id <> liked_id)
);

alter table public.likes enable row level security;

-- Beğeniler yalnızca RPC üzerinden yazılır; istemci kendi swipe'larını görebilir.
create policy "likes_select_own"
  on public.likes for select
  to authenticated
  using (liker_id = auth.uid());

-- ---------- MATCHES ----------
create table if not exists public.matches (
  id uuid primary key default gen_random_uuid(),
  user_a uuid not null references public.profiles (id) on delete cascade,
  user_b uuid not null references public.profiles (id) on delete cascade,
  created_at timestamptz not null default now(),
  unique (user_a, user_b),
  check (user_a < user_b)
);

alter table public.matches enable row level security;

create policy "matches_select_member"
  on public.matches for select
  to authenticated
  using (auth.uid() in (user_a, user_b));

-- ---------- MESSAGES ----------
create table if not exists public.messages (
  id uuid primary key,
  match_id uuid not null references public.matches (id) on delete cascade,
  sender_id uuid not null references public.profiles (id) on delete cascade,
  text text not null check (char_length(text) between 1 and 2000),
  created_at timestamptz not null default now()
);

create index if not exists messages_match_created_idx
  on public.messages (match_id, created_at);

alter table public.messages enable row level security;

create policy "messages_select_member"
  on public.messages for select
  to authenticated
  using (exists (
    select 1 from public.matches m
    where m.id = match_id and auth.uid() in (m.user_a, m.user_b)
  ));

create policy "messages_insert_member_as_self"
  on public.messages for insert
  to authenticated
  with check (
    sender_id = auth.uid()
    and exists (
      select 1 from public.matches m
      where m.id = match_id and auth.uid() in (m.user_a, m.user_b)
    )
  );

-- Realtime yayını (Realtime > Tables'tan da açılabilir)
do $$
begin
  alter publication supabase_realtime add table public.messages;
exception when duplicate_object then null;
end $$;

-- ---------- FEEDBACK ----------
create table if not exists public.feedback (
  id bigint generated always as identity primary key,
  user_id uuid references auth.users (id) on delete set null default auth.uid(),
  email text not null default '',
  rating int not null check (rating between 1 and 5),
  comment text not null default '',
  created_at timestamptz not null default now()
);

alter table public.feedback enable row level security;

create policy "feedback_insert_authenticated"
  on public.feedback for insert
  to authenticated
  with check (user_id = auth.uid());

-- ============================================================
-- RPC'ler
-- ============================================================

-- Keşif kuyruğu: benim olmayan, henüz swipe etmediğim profiller.
-- Uyum puanı basit bir müzik-zevki kesişimiyle hesaplanır.
create or replace function public.get_discover_profiles(p_limit int default 20)
returns table (
  id uuid,
  name text,
  age int,
  bio text,
  avatar_url text,
  favorite_genre text,
  top_artists text,
  top_tracks text,
  signature_song_id text,
  signature_song_title text,
  signature_song_artist text,
  signature_song_trim_start real,
  signature_song_trim_end real,
  compatibility int
)
language sql
security definer
set search_path = public
as $$
  with me as (
    select favorite_genre, top_artists from public.profiles where id = auth.uid()
  )
  select
    p.id, p.name, p.age, p.bio, p.avatar_url,
    p.favorite_genre, p.top_artists, p.top_tracks,
    p.signature_song_id, p.signature_song_title, p.signature_song_artist,
    p.signature_song_trim_start, p.signature_song_trim_end,
    least(98, 55
      + case when lower(p.favorite_genre) = lower((select favorite_genre from me)) then 30 else 0 end
      + case when (select top_artists from me) <> ''
              and p.signature_song_artist <> ''
              and position(lower(p.signature_song_artist) in lower((select top_artists from me))) > 0
             then 13 else 0 end
    )::int as compatibility
  from public.profiles p
  where p.id <> auth.uid()
    and p.name <> ''
    and not exists (
      select 1 from public.likes l
      where l.liker_id = auth.uid() and l.liked_id = p.id
    )
  order by compatibility desc, p.updated_at desc
  limit p_limit;
$$;

-- Swipe: beğeniyi kaydeder; karşılıklı beğeni varsa eşleşme oluşturur.
create or replace function public.handle_swipe(p_target uuid, p_is_like boolean)
returns table (matched boolean, match_id uuid)
language plpgsql
security definer
set search_path = public
as $$
declare
  v_me uuid := auth.uid();
  v_match_id uuid;
  v_a uuid;
  v_b uuid;
begin
  if v_me is null then
    raise exception 'not authenticated';
  end if;
  if p_target = v_me then
    raise exception 'cannot swipe yourself';
  end if;

  insert into public.likes (liker_id, liked_id, is_like)
  values (v_me, p_target, p_is_like)
  on conflict (liker_id, liked_id) do update set is_like = excluded.is_like;

  if p_is_like and exists (
    select 1 from public.likes
    where liker_id = p_target and liked_id = v_me and is_like
  ) then
    v_a := least(v_me, p_target);
    v_b := greatest(v_me, p_target);
    insert into public.matches (user_a, user_b)
    values (v_a, v_b)
    on conflict (user_a, user_b) do nothing;

    select m.id into v_match_id
    from public.matches m
    where m.user_a = v_a and m.user_b = v_b;

    return query select true, v_match_id;
    return;
  end if;

  return query select false, null::uuid;
end;
$$;

-- Eşleşmelerim: karşı tarafın profil özeti + son mesaj.
create or replace function public.get_my_matches()
returns table (
  match_id uuid,
  other_id uuid,
  other_name text,
  other_avatar_url text,
  created_at timestamptz,
  last_message text,
  last_message_at timestamptz
)
language sql
security definer
set search_path = public
as $$
  select
    m.id as match_id,
    p.id as other_id,
    p.name as other_name,
    p.avatar_url as other_avatar_url,
    m.created_at,
    coalesce(lm.text, '') as last_message,
    coalesce(lm.created_at, m.created_at) as last_message_at
  from public.matches m
  join public.profiles p
    on p.id = case when m.user_a = auth.uid() then m.user_b else m.user_a end
  left join lateral (
    select text, created_at
    from public.messages
    where match_id = m.id
    order by created_at desc
    limit 1
  ) lm on true
  where auth.uid() in (m.user_a, m.user_b)
  order by coalesce(lm.created_at, m.created_at) desc;
$$;

grant execute on function public.get_discover_profiles(int) to authenticated;
grant execute on function public.handle_swipe(uuid, boolean) to authenticated;
grant execute on function public.get_my_matches() to authenticated;

revoke execute on function public.get_discover_profiles(int) from anon;
revoke execute on function public.handle_swipe(uuid, boolean) from anon;
revoke execute on function public.get_my_matches() from anon;
