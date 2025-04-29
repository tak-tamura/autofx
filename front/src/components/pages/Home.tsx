import { FC } from "react";
import { Header } from "../organisms/layout/Header";
import { Outlet } from "react-router-dom";

export const Home: FC = () => {
    return (
        <>
            <Header />
            <Outlet />
        </>
    );
};