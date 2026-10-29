import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { MapPinIcon, PencilSimpleIcon, PlusIcon, TrashIcon } from "@phosphor-icons/react";
import api from "../../api/axios";
import { useTheme } from "../../context/ThemeContext";
import LoadingState from "../LoadingState";
import ErrorState from "../ErrorState";
import AddressForm from "../AddressForm";
import AddressLines from "../AddressLines";
import { errorMessage } from "../../utils/format.js";
import { btn, panel } from "../../utils/ui.js";

export default function AddressBook({ onCountChange }) {
    const { darkMode } = useTheme();
    const [addresses, setAddresses] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [editingId, setEditingId] = useState(null); // id, "new", or null
    const [deletingId, setDeletingId] = useState(null); // id awaiting confirm
    const [busyId, setBusyId] = useState(null);

    const load = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const { data } = await api.get("/user/addresses");
            setAddresses(data || []);
            onCountChange?.((data || []).length);
        } catch (err) {
            setError(errorMessage(err, "Could not load your addresses."));
        } finally {
            setLoading(false);
        }
    }, [onCountChange]);

    useEffect(() => {
        load();
    }, [load]);

    const handleCreate = async (values) => {
        try {
            await api.post("/user/addresses", values);
            toast.success("Address added");
            setEditingId(null);
            await load(); // server decides the default and order
        } catch (err) {
            toast.error(errorMessage(err, "Could not save the address"));
        }
    };

    const handleUpdate = async (id, values) => {
        try {
            const current = addresses.find((a) => a.id === id);
            await api.put(`/user/addresses/${id}`, {
                ...values,
                isDefault: current?.isDefault ?? false,
            });
            toast.success("Address updated");
            setEditingId(null);
            await load();
        } catch (err) {
            toast.error(errorMessage(err, "Could not update the address"));
        }
    };

    const handleDelete = async (id) => {
        setBusyId(id);
        try {
            await api.delete(`/user/addresses/${id}`);
            toast.success("Address deleted");
            setDeletingId(null);
            await load();
        } catch (err) {
            toast.error(errorMessage(err, "Could not delete the address"));
        } finally {
            setBusyId(null);
        }
    };

    const handleMakeDefault = async (id) => {
        setBusyId(id);
        try {
            await api.put(`/user/addresses/${id}/default`);
            toast.success("Default address updated");
            await load();
        } catch (err) {
            toast.error(errorMessage(err, "Could not change the default address"));
        } finally {
            setBusyId(null);
        }
    };

    return (
        <section className={`${panel(darkMode)} p-5 sm:p-6`} aria-labelledby="addressbook-h">
            <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
                <h2 id="addressbook-h" className="text-2xl font-black">
                    ADDRESS BOOK
                </h2>
                {editingId !== "new" && !loading && !error && (
                    <button
                        type="button"
                        onClick={() => setEditingId("new")}
                        className={btn(darkMode, "primary")}
                    >
                        <PlusIcon size={18} /> Add address
                    </button>
                )}
            </div>

            {editingId === "new" && (
                <div className="mb-6 p-4 rounded-lg border-4 border-black">
                    <h3 className="text-lg font-black mb-3">New address</h3>
                    <AddressForm onSubmit={handleCreate} onCancel={() => setEditingId(null)} />
                </div>
            )}

            {loading ? (
                <LoadingState variant="list" count={2} />
            ) : error ? (
                <ErrorState error={error} onRetry={load} />
            ) : addresses.length === 0 ? (
                editingId !== "new" && (
                    <div className="text-center py-8">
                        <MapPinIcon size={48} className="mx-auto mb-3 opacity-60" />
                        <p className="font-black text-lg">No saved addresses</p>
                        <p className="font-bold opacity-70">
                            Add one now to check out faster.
                        </p>
                    </div>
                )
            ) : (
                <ul className="space-y-4">
                    {addresses.map((a) => (
                        <li
                            key={a.id}
                            className={`p-4 rounded-lg border-4 ${a.isDefault ? "border-black" : darkMode ? "border-gray-600" : "border-gray-300"}`}
                        >
                            {editingId === a.id ? (
                                <AddressForm
                                    initial={a}
                                    onSubmit={(values) => handleUpdate(a.id, values)}
                                    onCancel={() => setEditingId(null)}
                                    submitLabel="Save changes"
                                />
                            ) : (
                                <>
                                    <div className="flex flex-wrap justify-between gap-3">
                                        <AddressLines address={a} />
                                        {a.isDefault && (
                                            <span className="self-start px-2 py-0.5 text-xs font-black border-2 border-black rounded bg-yellow-300 text-black">
                                                DEFAULT
                                            </span>
                                        )}
                                    </div>

                                    {deletingId === a.id ? (
                                        <div className="mt-4 flex flex-wrap items-center gap-3" role="alertdialog" aria-label="Confirm delete">
                                            <p className="font-black w-full">Delete this address?</p>
                                            <button
                                                type="button"
                                                onClick={() => handleDelete(a.id)}
                                                disabled={busyId === a.id}
                                                className={btn(darkMode, "danger")}
                                            >
                                                {busyId === a.id ? "Deleting..." : "Yes, delete"}
                                            </button>
                                            <button
                                                type="button"
                                                onClick={() => setDeletingId(null)}
                                                className={btn(darkMode, "secondary")}
                                            >
                                                Keep it
                                            </button>
                                        </div>
                                    ) : (
                                        <div className="mt-4 flex flex-wrap gap-3">
                                            <button
                                                type="button"
                                                onClick={() => setEditingId(a.id)}
                                                className={btn(darkMode, "secondary")}
                                                aria-label={`Edit address for ${a.fullName}, ${a.line1}`}
                                            >
                                                <PencilSimpleIcon size={18} /> Edit
                                            </button>
                                            {!a.isDefault && (
                                                <button
                                                    type="button"
                                                    onClick={() => handleMakeDefault(a.id)}
                                                    disabled={busyId === a.id}
                                                    className={btn(darkMode, "secondary")}
                                                >
                                                    Make default
                                                </button>
                                            )}
                                            <button
                                                type="button"
                                                onClick={() => setDeletingId(a.id)}
                                                className={btn(darkMode, "danger")}
                                                aria-label={`Delete address for ${a.fullName}, ${a.line1}`}
                                            >
                                                <TrashIcon size={18} /> Delete
                                            </button>
                                        </div>
                                    )}
                                </>
                            )}
                        </li>
                    ))}
                </ul>
            )}
        </section>
    );
}
