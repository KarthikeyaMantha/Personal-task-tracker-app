/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        coral: {
          DEFAULT: '#FF385C',
          light: '#FF5E3A',
          dark: '#E0264A',
          container: '#FFE8EC',
          containerDark: '#3A141D'
        },
        charcoal: {
          bg: '#121214',
          surface: '#18181B',
          card: '#202024',
          border: '#2E2E34'
        }
      },
      fontFamily: {
        sans: [
          '-apple-system',
          'BlinkMacSystemFont',
          '"SF Pro Display"',
          '"SF Pro Text"',
          '"Segoe UI"',
          'Roboto',
          'Helvetica',
          'Arial',
          'sans-serif'
        ]
      }
    },
  },
  plugins: [],
}

