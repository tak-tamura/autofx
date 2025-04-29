import { Charts } from "../components/pages/Charts";
import { Home } from "../components/pages/Home";
import { Setting } from "../components/pages/Setting";

export const homeRoutes = [
    {
        path: "chart",
        index: true,
        children: <Charts />,
    },
    {
        path: "setting",
        index: false,
        children: <Setting />,
    },
];