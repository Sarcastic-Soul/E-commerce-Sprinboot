import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { MapPinIcon, PencilSimpleIcon, PlusIcon, TrashIcon } from "@phosphor-icons/react";
import api from "../../api/axios";
import { useTheme } from "../../context/ThemeContext";
import LoadingState from "../LoadingState";
import ErrorState from "../ErrorState";
import AddressForm from "../AddressForm";
import AddressLines from "../AddressLines";
import type { Address } from "../../api/types";
import type { AddressFormPayload } from "../AddressForm";
import { errorMessage } from "../../utils/format";
import { btn, panel } from "../../utils/ui";

interface AddressBookProps {
    onCountChange?: (count: number) => void;
}

export default function AddressBook({ onCountChange }: AddressBookProps) {
    const { darkMode } = useTheme();
    const queryClient = useQueryClient();
    const [editingId, setEditingId] = useState<number | "new" | null>(null); // id, "new", or null
    const [deletingId, setDeletingId] = useState<number | null>(null); // id awaiting confirm
    const [busyId, setBusyId] = useState<number | null>(null);

    const {
        data,
        isPending: loading,
        error: queryError,
        refetch,
    } = useQuery({
        queryKey: ["addresses"],
        queryFn: async () => (await api.get<Address[]>("/user/addresses")).data || [],
    });
    const addresses = data ?? [];
    const error = queryError ? errorMessage(queryError, "Could not load your addresses.") : null;

    useEffect(() => {
        if (data) onCountChange?.(data.length);
    }, [data, onCountChange]);

    // server decides the default and order, so refetch after every write
    const reload = () => queryClient.invalidateQueries({ queryKey: ["addresses"] });

    const createAddress = useMutation({
        mutationFn: (values: AddressFormPayload) => api.post("/user/addresses", values),
        onSuccess: reload,
    });
    const updateAddress = useMutation({
        mutationFn: ({ id, body }: { id: number; body: AddressFormPayload & { isDefault: boolean } }) => api.put(`/user/addresses/${id}`, body),
        onSuccess: reload,
    });
    const deleteAddress = useMutation({
        mutationFn: (id: number) => api.delete(`/user/addresses/${id}`),
        onSuccess: reload,
    });
    const defaultAddress = useMutation({
        mutationFn: (id: number) => api.put(`/user/addresses/${id}/default`),
        onSuccess: reload,
    });

    const handleCreate = async (values: AddressFormPayload) => {
        try {
            await createAddress.mutateAsync(values);
            toast.success("Address added");
            setEditingId(null);
        } catch (err) {
            toast.error(errorMessage(err, "Could not save the address"));
        }
    };

    const handleUpdate = async (id: number, values: AddressFormPayload) => {
        try {
            const current = addresses.find((a) => a.id === id);
            await updateAddress.mutateAsync({
                id,
                body: { ...values, isDefault: current?.isDefault ?? false },
            });
            toast.success("Address updated");
            setEditingId(null);
        } catch (err) {
            toast.error(errorMessage(err, "Could not update the address"));
        }
    };

    const handleDelete = async (id: number) => {
        setBusyId(id);
        try {
            await deleteAddress.mutateAsync(id);
            toast.success("Address deleted");
            setDeletingId(null);
        } catch (err) {
            toast.error(errorMessage(err, "Could not delete the address"));
        } finally {
            setBusyId(null);
        }
    };

    const handleMakeDefault = async (id: number) => {
        setBusyId(id);
        try {
            await defaultAddress.mutateAsync(id);
            toast.success("Default address updated");
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
                <ErrorState error={error} onRetry={() => refetch()} />
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
