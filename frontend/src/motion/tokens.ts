export const easeOut = [0.22, 1, 0.36, 1] as const;

export const slidePage = {
  initial: { x: 56 },
  animate: { x: 0 },
  exit: { x: -40 },
  transition: { duration: 0.45, ease: easeOut }
};

export const slideUp = {
  initial: { y: 36 },
  animate: { y: 0 },
  transition: { duration: 0.55, ease: easeOut }
};

export const stagger = {
  animate: {
    transition: { staggerChildren: 0.08, delayChildren: 0.06 }
  }
};

export const hoverLift = {
  y: -6,
  scale: 1.02,
  transition: { duration: 0.22, ease: easeOut }
};
