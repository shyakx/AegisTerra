import { motion, useReducedMotion } from 'framer-motion';
import type { ReactNode } from 'react';
import { easeOut } from './tokens';

type Props = {
  children: ReactNode;
  className?: string;
  delay?: number;
  x?: number;
  y?: number;
};

export function Reveal({ children, className, delay = 0, x = 0, y = 28 }: Props) {
  const reduce = useReducedMotion();
  return (
    <motion.div
      className={className}
      initial={reduce ? false : { x, y }}
      whileInView={{ x: 0, y: 0 }}
      viewport={{ once: true, margin: '-80px' }}
      transition={{ duration: 0.55, ease: easeOut, delay }}
    >
      {children}
    </motion.div>
  );
}

export function Stagger({ children, className }: { children: ReactNode; className?: string }) {
  const reduce = useReducedMotion();
  return (
    <motion.div
      className={className}
      initial="hidden"
      whileInView="show"
      viewport={{ once: true, margin: '-60px' }}
      variants={{
        hidden: {},
        show: { transition: { staggerChildren: reduce ? 0 : 0.08 } }
      }}
    >
      {children}
    </motion.div>
  );
}

export function StaggerItem({ children, className }: { children: ReactNode; className?: string }) {
  const reduce = useReducedMotion();
  return (
    <motion.div
      className={className}
      variants={{
        hidden: reduce ? { y: 0 } : { y: 32 },
        show: { y: 0, transition: { duration: 0.5, ease: easeOut } }
      }}
    >
      {children}
    </motion.div>
  );
}
