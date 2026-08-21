import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import { useEffect, useState } from 'react';

export function RotatingPhrase({ words, className = '' }: { words: string[]; className?: string }) {
  const reduce = useReducedMotion();
  const [index, setIndex] = useState(0);

  useEffect(() => {
    if (reduce || words.length < 2) return undefined;
    const id = window.setInterval(() => setIndex((i) => (i + 1) % words.length), 2800);
    return () => window.clearInterval(id);
  }, [reduce, words.length]);

  const longest = words.reduce((a, b) => (a.length >= b.length ? a : b));
  const word = words[index];

  return (
    <span className={`inline-grid ${className}`}>
      <span className="invisible col-start-1 row-start-1" aria-hidden>
        {longest}
      </span>
      <AnimatePresence mode="wait">
        <motion.span
          key={word}
          className="col-start-1 row-start-1"
          initial={reduce ? false : { y: 8, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          exit={reduce ? undefined : { y: -8, opacity: 0 }}
          transition={{ duration: 0.35 }}
        >
          {word}
        </motion.span>
      </AnimatePresence>
    </span>
  );
}
