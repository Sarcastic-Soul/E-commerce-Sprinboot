import { createContext, useContext, useState, useEffect } from 'react';
import axios from '../api/axios';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [token, setToken] = useState(localStorage.getItem('token'));

    useEffect(() => {
        if (token) {
            try {
                const payload = JSON.parse(atob(token.split('.')[1]));
                const role = payload.role || (Array.isArray(payload.roles) ? payload.roles[0] : undefined);
                setUser({ username: payload.sub, role });
            } catch (e) {
                console.error('Invalid token', e);
                logout(); // clear junk token
            }
        } else {
            setUser(null);
        }
    }, [token]);

    const saveToken = (token) => {
        localStorage.setItem('token', token);
        setToken(token);
    };

    const login = async (username, password) => {
        try {
            const res = await axios.post('/auth/login', { username, password });
            saveToken(res.data.token);
        } catch (err) {
            throw new Error('Login failed');
        }
    };

    const signup = async (username, password) => {
        try {
            await axios.post('/auth/signup', { username, password });
            await login(username, password); // auto-login
        } catch (err) {
            const message = err.response?.data || 'Signup failed';
            throw new Error(message);
        }
    };

    const logout = () => {
        localStorage.removeItem('token');
        setToken(null);
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, token, login, signup, logout }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);
