import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { UserCircleIcon } from "@phosphor-icons/react";
import api from "../api/axios";
import { useTheme } from "../context/ThemeContext";
import LoadingState from "../components/LoadingState";
import ErrorState from "../components/ErrorState";
import PageHeader from "../components/PageHeader";
import ChangePasswordForm from "../components/account/ChangePasswordForm";
import AddressBook from "../components/account/AddressBook";
import { errorMessage } from "../utils/format.js";
import { focusRing, panel } from "../utils/ui.js";

export default function AccountPage() {
    const { darkMode } = useTheme();
    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const load = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const { data } = await api.get("/user/profile");
            setProfile(data);
        } catch (err) {
            setError(errorMessage(err, "Could not load your profile."));
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        load();
    }, [load]);

    // Keep the address count in the summary in step with the address book.
    const handleAddressCount = useCallback((count) => {
        setProfile((p) => (p ? { ...p, addressCount: count } : p));
    }, []);

    const roles = profile?.roles
        ? (Array.isArray(profile.roles) ? profile.roles : [profile.roles])
              .map((r) => String(r).replace(/^ROLE_/, ""))
              .join(", ")
        : "";

    const stats = profile
        ? [
              { label: "Orders", value: profile.orderCount, to: "/orders", color: "bg-green-400" },
              { label: "Reviews", value: profile.reviewCount, color: "bg-pink-400" },
              { label: "Addresses", value: profile.addressCount, color: "bg-yellow-400" },
          ]
        : [];

    return (
        <div className="max-w-4xl mx-auto mt-8 pb-12 space-y-8">
            <PageHeader title="MY ACCOUNT" />

            {loading ? (
                <LoadingState variant="list" count={2} />
            ) : error ? (
                <ErrorState error={error} onRetry={load} />
            ) : (
                <section className={`${panel(darkMode)} p-5 sm:p-6`} aria-labelledby="profile-h">
                    <div className="flex items-center gap-4 mb-6">
                        <UserCircleIcon size={56} className="shrink-0" aria-hidden="true" />
                        <div className="min-w-0">
                            <h2 id="profile-h" className="text-2xl font-black truncate">
                                {profile.username}
                            </h2>
                            <p className="font-bold opacity-70">{roles}</p>
                        </div>
                    </div>
                    <div className="grid grid-cols-3 gap-3 sm:gap-6">
                        {stats.map((s) => {
                            const body = (
                                <>
                                    <span className="block font-black text-[11px] sm:text-xs tracking-wide sm:tracking-widest opacity-80 truncate">
                                        {s.label.toUpperCase()}
                                    </span>
                                    <span className="block text-3xl font-black">{s.value ?? 0}</span>
                                </>
                            );
                            const cls = `block p-3 sm:p-4 border-4 border-black rounded-lg shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] text-black ${s.color}`;
                            return s.to ? (
                                <Link key={s.label} to={s.to} className={`${cls} hover:underline ${focusRing}`}>
                                    {body}
                                </Link>
                            ) : (
                                <div key={s.label} className={cls}>
                                    {body}
                                </div>
                            );
                        })}
                    </div>
                </section>
            )}

            <AddressBook onCountChange={handleAddressCount} />
            <ChangePasswordForm />
        </div>
    );
}
