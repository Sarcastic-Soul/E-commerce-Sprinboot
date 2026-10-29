import { Component } from "react";

// A render-time throw anywhere below this (bad API shape, null field, a genuine
// bug) used to whitescreen the whole app. Error boundaries must be class
// components — there is no hook equivalent.
export default class ErrorBoundary extends Component {
    constructor(props) {
        super(props);
        this.state = { hasError: false };
    }

    static getDerivedStateFromError() {
        return { hasError: true };
    }

    componentDidCatch(error, info) {
        console.error("Unhandled render error:", error, info);
    }

    render() {
        if (this.state.hasError) {
            return (
                <div className="flex flex-col items-center justify-center min-h-screen gap-4 text-center p-8">
                    <h1 className="text-3xl font-black">SOMETHING BROKE</h1>
                    <p className="opacity-70">
                        Reload the page to try again.
                    </p>
                    <button
                        onClick={() => window.location.reload()}
                        className="px-6 py-3 border-4 border-black rounded-lg font-bold bg-yellow-400 hover:bg-yellow-500 shadow-[4px_4px_0px_0px_black] transition-transform active:translate-x-[2px] active:translate-y-[2px] active:shadow-none"
                    >
                        RELOAD
                    </button>
                </div>
            );
        }
        return this.props.children;
    }
}
