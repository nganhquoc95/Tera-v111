import { SVGAttributes } from 'react';

export default function AppLogoIcon(props: SVGAttributes<SVGElement>) {
    return (
        <svg {...props} viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg">
            {/* Maple Leaf Shape */}
            <path
                fillRule="evenodd"
                clipRule="evenodd"
                d="M20 2C20 2 22 8 28 10C34 12 35 18 32 22C30 25 25 26 20 26C15 26 10 25 8 22C5 18 6 12 12 10C18 8 20 2 20 2Z"
                fill="currentColor"
            />
            {/* Left leaf point */}
            <path
                fillRule="evenodd"
                clipRule="evenodd"
                d="M8 18C6 20 2 22 1 28C0 32 4 36 10 36C12 36 14 35 15 33C13 32 9 28 8 24C7 21 8 18 8 18Z"
                fill="currentColor"
            />
            {/* Right leaf point */}
            <path
                fillRule="evenodd"
                clipRule="evenodd"
                d="M32 18C34 20 38 22 39 28C40 32 36 36 30 36C28 36 26 35 25 33C27 32 31 28 32 24C33 21 32 18 32 18Z"
                fill="currentColor"
            />
            {/* Bottom left leaf point */}
            <path
                fillRule="evenodd"
                clipRule="evenodd"
                d="M12 28C10 30 6 34 8 38C10 40 16 40 18 38C17 36 15 32 14 28C13 27 12 28 12 28Z"
                fill="currentColor"
            />
            {/* Bottom right leaf point */}
            <path
                fillRule="evenodd"
                clipRule="evenodd"
                d="M28 28C30 30 34 34 32 38C30 40 24 40 22 38C23 36 25 32 26 28C27 27 28 28 28 28Z"
                fill="currentColor"
            />
        </svg>
    );
}
