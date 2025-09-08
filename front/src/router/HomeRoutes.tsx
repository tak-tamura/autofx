import { Charts } from "../components/pages/Charts";
import { TradeSetting } from "../components/pages/TradeSetting";

export const homeRoutes = [
    {
        path: "chart",
        index: true,
        children: <Charts />,
    },
    {
        path: "setting",
        index: false,
        children: <TradeSetting />,
    },
];