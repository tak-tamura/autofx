import { FC, memo } from "react";
import { Route, Routes } from "react-router-dom";

import { homeRoutes } from "./HomeRoutes";
import { Home } from "../components/pages/Home"
import { Charts } from "../components/pages/Charts";
import {LoginPage} from "../components/pages/LoginPage";
import PrivateRoute from "../components/atoms/PrivateRoute";

export const Router: FC = memo(() => {
    return (
        <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/home" element={<PrivateRoute component={Home}/>}>
                <Route index element={<Charts />} />
                {homeRoutes.map((route, index) => (
                    <Route
                        key={index}
                        path={route.path}
                        index={route.index}
                        element={route.children}
                    />
                ))};
            </Route>
        </Routes>
    );
});