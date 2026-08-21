type Props = {
  src: string;
  alt: string;
  className?: string;
  imgClassName?: string;
  kenBurns?: boolean;
  priority?: boolean;
};

export function PhotoStage({ src, alt, className = '', imgClassName = '', kenBurns = false, priority = false }: Props) {
  return (
    <div className={`overflow-hidden bg-canopy ${className}`}>
      <img
        src={src}
        alt={alt}
        width={1600}
        height={900}
        loading={priority ? 'eager' : 'lazy'}
        decoding="async"
        aria-hidden={alt === '' ? true : undefined}
        className={`h-full w-full object-cover ${kenBurns ? 'at-kenburns' : ''} ${imgClassName}`}
      />
    </div>
  );
}
