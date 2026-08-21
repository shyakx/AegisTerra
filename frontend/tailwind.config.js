/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: '#0F5132',
        secondary: '#1B7F5C',
        accent: '#2F9E44',
        success: '#2E7D32',
        warning: '#F59E0B',
        danger: '#DC2626',
        info: '#2563EB',
        background: '#F8FAFC',
        surface: '#FFFFFF',
        sidebar: '#0B2E22',
        textPrimary: '#111827',
        textSecondary: '#6B7280',
        border: '#E5E7EB',
        void: '#0B2E22',
        canopy: '#0F5132',
        signal: '#1B7F5C',
        beam: '#2563EB',
        gold: '#2F9E44',
        leaf: '#D1FAE5'
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        display: ['Syne', 'Inter', 'system-ui', 'sans-serif']
      },
      boxShadow: {
        lift: '0 8px 24px rgba(15, 81, 50, 0.08)'
      }
    }
  },
  plugins: []
};
