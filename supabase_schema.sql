-- ====================================================================
-- SplitBill - CLEAN RESET & SETUP SCHEMA FOR SUPABASE
-- Chạy toàn bộ script này trong Supabase Dashboard -> SQL Editor -> New Query -> Run
-- ====================================================================

-- 1. Xóa toàn bộ bảng cũ và các ràng buộc cũ (Ktor era)
DROP TABLE IF EXISTS public.fcm_tokens CASCADE;
DROP TABLE IF EXISTS public.activity_logs CASCADE;
DROP TABLE IF EXISTS public.settlements CASCADE;
DROP TABLE IF EXISTS public.bill_splits CASCADE;
DROP TABLE IF EXISTS public.bills CASCADE;
DROP TABLE IF EXISTS public.group_members CASCADE;
DROP TABLE IF EXISTS public.groups CASCADE;
DROP TABLE IF EXISTS public.profiles CASCADE;
DROP TABLE IF EXISTS public.users CASCADE;

-- 2. Bật extension UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 3. Bảng profiles (Liên kết 1-1 với auth.users của Supabase)
CREATE TABLE public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT NOT NULL,
    username TEXT NOT NULL,
    avatar_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Tự động đồng bộ profile khi user đăng ký qua Supabase Auth
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, email, username, avatar_url)
    VALUES (
        NEW.id,
        COALESCE(NEW.email, ''),
        COALESCE(NEW.raw_user_meta_data->>'username', split_part(COALESCE(NEW.email, 'user'), '@', 1)),
        NEW.raw_user_meta_data->>'avatar_url'
    )
    ON CONFLICT (id) DO UPDATE
    SET email = EXCLUDED.email,
        username = COALESCE(EXCLUDED.username, public.profiles.username),
        updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Đồng bộ ngay tất cả users hiện có trong auth.users vào profiles
INSERT INTO public.profiles (id, email, username, avatar_url)
SELECT 
    id,
    COALESCE(email, ''),
    COALESCE(raw_user_meta_data->>'username', split_part(COALESCE(email, 'user'), '@', 1)),
    raw_user_meta_data->>'avatar_url'
FROM auth.users
ON CONFLICT (id) DO UPDATE
SET email = EXCLUDED.email,
    username = COALESCE(EXCLUDED.username, public.profiles.username),
    updated_at = NOW();

-- 4. Bảng groups
CREATE TABLE public.groups (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    description TEXT DEFAULT '',
    icon TEXT DEFAULT 'default',
    created_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. Bảng group_members
CREATE TABLE public.group_members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID NOT NULL REFERENCES public.groups(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role TEXT DEFAULT 'MEMBER', -- 'OWNER', 'ADMIN', 'MEMBER'
    joined_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(group_id, user_id)
);

-- 6. Bảng bills
CREATE TABLE public.bills (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID NOT NULL REFERENCES public.groups(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    total_amount NUMERIC(15, 2) NOT NULL,
    paid_by_user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    currency VARCHAR(10) DEFAULT 'VND',
    exchange_rate NUMERIC(15, 6) DEFAULT 1.0,
    category VARCHAR(50) DEFAULT 'OTHER',
    receipt_url TEXT,
    is_paid BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 7. Bảng bill_splits
CREATE TABLE public.bill_splits (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    bill_id UUID NOT NULL REFERENCES public.bills(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    amount_owed NUMERIC(15, 2) NOT NULL,
    is_settled BOOLEAN DEFAULT FALSE,
    UNIQUE(bill_id, user_id)
);

-- 8. Bảng settlements (Thanh toán nợ)
CREATE TABLE public.settlements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID NOT NULL REFERENCES public.groups(id) ON DELETE CASCADE,
    from_user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    to_user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    amount NUMERIC(15, 2) NOT NULL,
    note TEXT DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 9. Bảng activity_logs (Nhật ký hoạt động nhóm)
CREATE TABLE public.activity_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID NOT NULL REFERENCES public.groups(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    activity_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 10. Bảng fcm_tokens
CREATE TABLE public.fcm_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    token TEXT NOT NULL,
    device_type VARCHAR(20) DEFAULT 'android',
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, token)
);

-- ====================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ====================================================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.group_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bills ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bill_splits ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.settlements ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.activity_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.fcm_tokens ENABLE ROW LEVEL SECURITY;

-- Helper Function: Kiểm tra user có phải là thành viên nhóm không
CREATE OR REPLACE FUNCTION public.is_group_member(check_group_id UUID, check_user_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM public.group_members
        WHERE group_id = check_group_id AND user_id = check_user_id
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Profiles Policies
CREATE POLICY "Allow authenticated read profiles" ON public.profiles
    FOR SELECT TO authenticated USING (true);

CREATE POLICY "Allow user update own profile" ON public.profiles
    FOR UPDATE TO authenticated USING (auth.uid() = id);

CREATE POLICY "Allow user insert own profile" ON public.profiles
    FOR INSERT TO authenticated WITH CHECK (auth.uid() = id);

-- Groups Policies
CREATE POLICY "Allow members to view groups" ON public.groups
    FOR SELECT TO authenticated
    USING (public.is_group_member(id, auth.uid()) OR auth.uid() = created_by);

CREATE POLICY "Allow authenticated to create group" ON public.groups
    FOR INSERT TO authenticated
    WITH CHECK (auth.uid() = created_by);

CREATE POLICY "Allow group creator to update group" ON public.groups
    FOR UPDATE TO authenticated
    USING (auth.uid() = created_by);

CREATE POLICY "Allow group creator to delete group" ON public.groups
    FOR DELETE TO authenticated
    USING (auth.uid() = created_by);

-- Group Members Policies
CREATE POLICY "Allow members to view group members" ON public.group_members
    FOR SELECT TO authenticated
    USING (public.is_group_member(group_id, auth.uid()) OR EXISTS (SELECT 1 FROM public.groups g WHERE g.id = group_id AND g.created_by = auth.uid()));

CREATE POLICY "Allow creator or admin to add members" ON public.group_members
    FOR INSERT TO authenticated
    WITH CHECK (
        auth.uid() = user_id 
        OR public.is_group_member(group_id, auth.uid())
        OR EXISTS (SELECT 1 FROM public.groups g WHERE g.id = group_id AND g.created_by = auth.uid())
    );

CREATE POLICY "Allow member to leave or owner to remove" ON public.group_members
    FOR DELETE TO authenticated
    USING (auth.uid() = user_id OR public.is_group_member(group_id, auth.uid()));

-- Bills Policies
CREATE POLICY "Allow group members to view bills" ON public.bills
    FOR SELECT TO authenticated
    USING (public.is_group_member(group_id, auth.uid()));

CREATE POLICY "Allow group members to insert bills" ON public.bills
    FOR INSERT TO authenticated
    WITH CHECK (public.is_group_member(group_id, auth.uid()));

CREATE POLICY "Allow bill payer or group members to update bill" ON public.bills
    FOR UPDATE TO authenticated
    USING (public.is_group_member(group_id, auth.uid()));

CREATE POLICY "Allow bill payer or group members to delete bill" ON public.bills
    FOR DELETE TO authenticated
    USING (public.is_group_member(group_id, auth.uid()));

-- Bill Splits Policies
CREATE POLICY "Allow group members to view bill splits" ON public.bill_splits
    FOR SELECT TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.bills b
            WHERE b.id = bill_splits.bill_id
            AND public.is_group_member(b.group_id, auth.uid())
        )
    );

CREATE POLICY "Allow group members to insert bill splits" ON public.bill_splits
    FOR INSERT TO authenticated
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.bills b
            WHERE b.id = bill_splits.bill_id
            AND public.is_group_member(b.group_id, auth.uid())
        )
    );

CREATE POLICY "Allow group members to update bill splits" ON public.bill_splits
    FOR UPDATE TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.bills b
            WHERE b.id = bill_splits.bill_id
            AND public.is_group_member(b.group_id, auth.uid())
        )
    );

CREATE POLICY "Allow group members to delete bill splits" ON public.bill_splits
    FOR DELETE TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.bills b
            WHERE b.id = bill_splits.bill_id
            AND public.is_group_member(b.group_id, auth.uid())
        )
    );

-- Settlements Policies
CREATE POLICY "Allow group members to view settlements" ON public.settlements
    FOR SELECT TO authenticated
    USING (public.is_group_member(group_id, auth.uid()));

CREATE POLICY "Allow group members to create settlements" ON public.settlements
    FOR INSERT TO authenticated
    WITH CHECK (public.is_group_member(group_id, auth.uid()));

-- Activity Logs Policies
CREATE POLICY "Allow group members to view activity logs" ON public.activity_logs
    FOR SELECT TO authenticated
    USING (public.is_group_member(group_id, auth.uid()));

CREATE POLICY "Allow group members to insert activity logs" ON public.activity_logs
    FOR INSERT TO authenticated
    WITH CHECK (public.is_group_member(group_id, auth.uid()));

-- FCM Tokens Policies
CREATE POLICY "Allow user manage own fcm tokens" ON public.fcm_tokens
    FOR ALL TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ====================================================================
-- STORAGE BUCKETS (avatars & receipts)
-- ====================================================================
INSERT INTO storage.buckets (id, name, public) 
VALUES ('avatars', 'avatars', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO storage.buckets (id, name, public) 
VALUES ('receipts', 'receipts', true)
ON CONFLICT (id) DO NOTHING;

DROP POLICY IF EXISTS "Public Read Avatars" ON storage.objects;
CREATE POLICY "Public Read Avatars" ON storage.objects
    FOR SELECT USING (bucket_id = 'avatars');

DROP POLICY IF EXISTS "Authenticated Upload Avatars" ON storage.objects;
CREATE POLICY "Authenticated Upload Avatars" ON storage.objects
    FOR INSERT TO authenticated
    WITH CHECK (bucket_id = 'avatars');

DROP POLICY IF EXISTS "Public Read Receipts" ON storage.objects;
CREATE POLICY "Public Read Receipts" ON storage.objects
    FOR SELECT USING (bucket_id = 'receipts');

DROP POLICY IF EXISTS "Authenticated Upload Receipts" ON storage.objects;
CREATE POLICY "Authenticated Upload Receipts" ON storage.objects
    FOR INSERT TO authenticated
    WITH CHECK (bucket_id = 'receipts');
