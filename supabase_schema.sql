-- MediTrack AI : Supabase Schema
-- Run this in your Supabase SQL Editor

-- 1. Create Profiles Custom Table
CREATE TABLE public.profiles (
  id UUID REFERENCES auth.users(id) ON DELETE CASCADE PRIMARY KEY,
  email TEXT NOT NULL,
  full_name TEXT,
  age INTEGER,
  gender TEXT,
  mobile_number TEXT,
  address TEXT,
  emergency_contact TEXT,
  profile_picture_url TEXT,
  medical_conditions TEXT,
  allergies TEXT,
  role TEXT DEFAULT 'user' CHECK (role IN ('user', 'admin')),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. Create Medicines Table
CREATE TABLE public.medicines (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  profile_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
  name TEXT NOT NULL,
  type TEXT NOT NULL CHECK (type IN ('Tablet', 'Syrup', 'Injection', 'Capsule')),
  dosage TEXT NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  frequency TEXT NOT NULL, -- e.g., 'Daily', 'Weekly'
  notes TEXT,
  image_url TEXT,
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3. Create Reminders Table
CREATE TABLE public.reminders (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  medicine_id UUID REFERENCES public.medicines(id) ON DELETE CASCADE NOT NULL,
  time TIME NOT NULL,
  grouping TEXT NOT NULL CHECK (grouping IN ('Morning', 'Afternoon', 'Night')),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 4. Create Medicine Logs Table
CREATE TABLE public.medicine_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  medicine_id UUID REFERENCES public.medicines(id) ON DELETE CASCADE NOT NULL,
  reminder_id UUID REFERENCES public.reminders(id) ON DELETE CASCADE NOT NULL,
  status TEXT NOT NULL CHECK (status IN ('Taken', 'Missed', 'Skipped')),
  scheduled_time TIMESTAMP WITH TIME ZONE NOT NULL,
  actual_time TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 5. Create Health Logs Table
CREATE TABLE public.health_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  profile_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
  log_type TEXT NOT NULL CHECK (log_type IN ('Blood Pressure', 'Sugar Level', 'Water Intake', 'Weight', 'Heart Rate')),
  value TEXT NOT NULL,
  unit TEXT,
  notes TEXT,
  recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 6. Create Admin Announcements Table
CREATE TABLE public.admin_announcements (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  admin_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
  title TEXT NOT NULL,
  content TEXT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 7. Create Notifications Table (for in-app alerts)
CREATE TABLE public.notifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  profile_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
  title TEXT NOT NULL,
  message TEXT NOT NULL,
  is_read BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Set up Row Level Security (RLS)

-- Enable RLS
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.medicines ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reminders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.medicine_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.health_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.admin_announcements ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

-- Profiles Policies
CREATE POLICY "Users can view their own profile" ON public.profiles FOR SELECT USING (auth.uid() = id);
CREATE POLICY "Admins can view all profiles" ON public.profiles FOR SELECT USING (EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role = 'admin'));
CREATE POLICY "Users can insert their own profile" ON public.profiles FOR INSERT WITH CHECK (auth.uid() = id);
CREATE POLICY "Users can update their own profile" ON public.profiles FOR UPDATE USING (auth.uid() = id);

-- Medicines Policies
CREATE POLICY "Users can view their own medicines" ON public.medicines FOR SELECT USING (auth.uid() = profile_id);
CREATE POLICY "Admins can view all medicines" ON public.medicines FOR SELECT USING (EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role = 'admin'));
CREATE POLICY "Users can insert their own medicines" ON public.medicines FOR INSERT WITH CHECK (auth.uid() = profile_id);
CREATE POLICY "Users can update their own medicines" ON public.medicines FOR UPDATE USING (auth.uid() = profile_id);
CREATE POLICY "Users can delete their own medicines" ON public.medicines FOR DELETE USING (auth.uid() = profile_id);

-- Reminders Policies
CREATE POLICY "Users can view reminders for their medicines" ON public.reminders FOR SELECT USING (EXISTS (SELECT 1 FROM public.medicines WHERE id = medicine_id AND profile_id = auth.uid()));
CREATE POLICY "Users can insert reminders for their medicines" ON public.reminders FOR INSERT WITH CHECK (EXISTS (SELECT 1 FROM public.medicines WHERE id = medicine_id AND profile_id = auth.uid()));
CREATE POLICY "Users can update reminders for their medicines" ON public.reminders FOR UPDATE USING (EXISTS (SELECT 1 FROM public.medicines WHERE id = medicine_id AND profile_id = auth.uid()));
CREATE POLICY "Users can delete reminders for their medicines" ON public.reminders FOR DELETE USING (EXISTS (SELECT 1 FROM public.medicines WHERE id = medicine_id AND profile_id = auth.uid()));

-- Medicine Logs Policies
CREATE POLICY "Users can view their medicine logs" ON public.medicine_logs FOR SELECT USING (EXISTS (SELECT 1 FROM public.medicines WHERE id = medicine_id AND profile_id = auth.uid()));
CREATE POLICY "Users can insert their medicine logs" ON public.medicine_logs FOR INSERT WITH CHECK (EXISTS (SELECT 1 FROM public.medicines WHERE id = medicine_id AND profile_id = auth.uid()));
CREATE POLICY "Users can update their medicine logs" ON public.medicine_logs FOR UPDATE USING (EXISTS (SELECT 1 FROM public.medicines WHERE id = medicine_id AND profile_id = auth.uid()));

-- Health Logs Policies
CREATE POLICY "Users can view their own health logs" ON public.health_logs FOR SELECT USING (auth.uid() = profile_id);
CREATE POLICY "Users can insert their own health logs" ON public.health_logs FOR INSERT WITH CHECK (auth.uid() = profile_id);
CREATE POLICY "Users can update their own health logs" ON public.health_logs FOR UPDATE USING (auth.uid() = profile_id);
CREATE POLICY "Users can delete their own health logs" ON public.health_logs FOR DELETE USING (auth.uid() = profile_id);

-- Notifications Policies
CREATE POLICY "Users can view their own notifications" ON public.notifications FOR SELECT USING (auth.uid() = profile_id);
CREATE POLICY "Users can update their own notifications" ON public.notifications FOR UPDATE USING (auth.uid() = profile_id);

-- Admin Announcements Policies
CREATE POLICY "Anyone can view announcements" ON public.admin_announcements FOR SELECT USING (TRUE);
CREATE POLICY "Admins can insert announcements" ON public.admin_announcements FOR INSERT WITH CHECK (EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role = 'admin'));

-- Functions and Triggers
-- Handle new user creation: auto-insert into profiles when signup via auth.users happens
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.profiles (id, email, full_name, role)
  VALUES (new.id, new.email, raw_user_meta_data->>'full_name', 'user');
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE PROCEDURE public.handle_new_user();

-- Storage setup guidelines
-- Note: Create buckets manually in dashboard or use SQL if superuser access exists.
-- Buckets: 'profile-images', 'medicine-images'
-- Polices to set in bucket storage (Using UI or SQL):
-- 1. profile-images: Authenticated read/write.
-- 2. medicine-images: Authenticated read/write.
