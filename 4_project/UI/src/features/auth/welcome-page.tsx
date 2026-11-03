import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  ArrowRight,
  ArrowUpRight,
  Braces,
  Globe2,
  KeyRound,
  Loader2,
  Moon,
  Sun,
} from "lucide-react";
import { z } from "zod";
import { motion } from "motion/react";
import { registrationSchema, type RegistrationValues } from "./schemas";
import { authApi, usersApi } from "./api";
import { Brand } from "@/components/navigation/brand";
import { Button } from "@/components/ui/button";
import { Field, Input } from "@/components/ui/field";
import { ErrorMessage } from "@/components/shared/feedback";
import { useTheme } from "@/app/providers/app-providers";

function RegistrationForm() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<RegistrationValues>({
    resolver: zodResolver(registrationSchema),
  });
  const mutation = useMutation({
    mutationFn: usersApi.register,
    onSuccess: (user) => {
      reset();
      queryClient.removeQueries({ queryKey: ["session"] });
      navigate(`/users/${user.id}/applications`);
    },
  });
  return (
    <form
      className="form-stack"
      onSubmit={handleSubmit((values) => mutation.mutate(values))}
    >
      <Field label="Full name" id="name" error={errors.name?.message}>
        <Input
          id="name"
          placeholder="Alex Morgan"
          autoComplete="name"
          {...register("name")}
          aria-invalid={!!errors.name}
        />
      </Field>
      <Field label="Email address" id="email" error={errors.email?.message}>
        <Input
          id="email"
          type="email"
          placeholder="alex@company.com"
          autoComplete="email"
          {...register("email")}
          aria-invalid={!!errors.email}
        />
      </Field>
      <Field
        label="Password"
        id="password"
        error={errors.password?.message}
        hint="Use 12–128 characters."
      >
        <Input
          id="password"
          type="password"
          autoComplete="new-password"
          {...register("password")}
          aria-invalid={!!errors.password}
        />
      </Field>
      <ErrorMessage error={mutation.error} />
      <Button type="submit" disabled={mutation.isPending}>
        {mutation.isPending ? (
          <Loader2 size={16} className="animate-spin" />
        ) : null}
        Create your workspace
        <ArrowRight size={16} />
      </Button>
    </form>
  );
}
const loginSchema = z.object({
  email: z.string().trim().email("Enter a valid email address."),
  password: z.string().min(1, "Enter your password.").max(128),
});
function LoginForm() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<z.infer<typeof loginSchema>>({
    resolver: zodResolver(loginSchema),
  });
  const mutation = useMutation({
    mutationFn: authApi.login,
    onSuccess: (session) => {
      reset();
      queryClient.setQueryData(["session"], session);
      navigate(`/users/${session.user.id}/applications`, { replace: true });
    },
  });
  return (
    <form
      className="form-stack"
      onSubmit={handleSubmit((values) => mutation.mutate(values))}
    >
      <Field
        label="Email address"
        id="login-email"
        error={errors.email?.message}
      >
        <Input
          id="login-email"
          type="email"
          autoComplete="username"
          {...register("email")}
        />
      </Field>
      <Field
        label="Password"
        id="login-password"
        error={errors.password?.message}
      >
        <Input
          id="login-password"
          type="password"
          autoComplete="current-password"
          {...register("password")}
        />
      </Field>
      <ErrorMessage error={mutation.error} />
      <Button type="submit" disabled={mutation.isPending}>
        Sign in
        <ArrowRight size={16} />
      </Button>
    </form>
  );
}
export default function WelcomePage() {
  const [tab, setTab] = useState<"register" | "open">("register");
  const { dark, toggle } = useTheme();
  const session = useQuery({
    queryKey: ["session"],
    queryFn: authApi.me,
    retry: false,
  });
  if (session.data && !session.isError)
    return (
      <Navigate to={`/users/${session.data.user.id}/applications`} replace />
    );
  return (
    <div className="welcome">
      <aside className="welcome-story">
        <Link to="/" aria-label="Event Relay home">
          <Brand inverse />
        </Link>
        <motion.div
          className="welcome-pitch"
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
        >
          <span className="story-label">
            <span />A home for your webhooks
          </span>
          <h1>
            Every event starts
            <br />
            with a connection.
          </h1>
          <p>
            Bring your applications, event types, and destinations together. One
            clear workspace for the infrastructure you’re building.
          </p>
          <div className="connection-visual" aria-hidden="true">
            <div className="connection-node">
              <Braces size={22} />
              <span>Your application</span>
            </div>
            <div className="connection-track">
              <span />
              <span />
              <span />
            </div>
            <div className="connection-node destination">
              <Globe2 size={22} />
              <span>Your endpoints</span>
            </div>
          </div>
          <div className="story-features">
            <span>
              <Braces size={15} />
              Define your events
            </span>
            <span>
              <Globe2 size={15} />
              Connect destinations
            </span>
            <span>
              <KeyRound size={15} />
              Manage access keys
            </span>
          </div>
        </motion.div>
        <div className="story-footer">
          <span>Built for the way developers work.</span>
          <span>
            Event Relay <ArrowUpRight size={13} />
          </span>
        </div>
      </aside>
      <main className="welcome-main">
        <div className="welcome-top">
          <span className="badge">Workspace console</span>
          <Button
            variant="ghost"
            size="icon"
            onClick={toggle}
            aria-label={dark ? "Switch to light theme" : "Switch to dark theme"}
          >
            {dark ? <Sun size={18} /> : <Moon size={18} />}
          </Button>
        </div>
        <div className="welcome-form">
          <p className="eyebrow">Start building</p>
          <h2>
            {tab === "register" ? "Create your workspace" : "Welcome back"}
          </h2>
          <p className="page-description">
            {tab === "register"
              ? "A little setup. A lot of possibilities."
              : "Sign in with your email and password."}
          </p>
          <div className="segmented" role="group" aria-label="Workspace access">
            <button
              aria-pressed={tab === "register"}
              onClick={() => setTab("register")}
            >
              Create account
            </button>
            <button
              aria-pressed={tab === "open"}
              onClick={() => setTab("open")}
            >
              Sign in
            </button>
          </div>
          {tab === "register" ? <RegistrationForm /> : <LoginForm />}
          <p className="welcome-note">
            Your session stays signed in after a refresh. Sign out on shared
            devices.
          </p>
        </div>
        <footer className="welcome-bottom">
          Event Relay{" "}
          <span>Webhook infrastructure, thoughtfully connected.</span>
        </footer>
      </main>
    </div>
  );
}
