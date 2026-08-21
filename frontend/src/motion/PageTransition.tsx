import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import type { ReactNode } from 'react';
import { useLocation } from 'react-router-dom';
import { easeOut } from './tokens';

export function PageTransition({ children }: { children: ReactNode }) {
  const location = useLocation();
  const reduce = useReducedMotion();

  return (
    <div className="overflow-hidden">
      <AnimatePresence mode="wait">
        <motion.div
          key={location.pathname}
          initial={reduce ? false : { x: 48 }}
          animate={{ x: 0 }}
          exit={reduce ? undefined : { x: -36 }}
          transition={{ duration: 0.42, ease: easeOut }}
        >
          {children}
        </motion.div>
      </AnimatePresence>
    </div>
  );
}
