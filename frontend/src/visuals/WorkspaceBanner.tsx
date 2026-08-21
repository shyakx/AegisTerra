import type { ReactNode } from 'react';
import { PhotoStage } from './PhotoStage';

type Props = {
  photo: { src: string; alt?: string };
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: ReactNode;
};

export function WorkspaceBanner({ photo, eyebrow, title, description, actions }: Props) {
  return (
    <section className="relative overflow-hidden rounded-3xl bg-sidebar text-white">
      <PhotoStage
        src={photo.src}
        alt={photo.alt ?? ''}
        className="absolute inset-0 min-h-[220px]"
        imgClassName="object-[72%_center]"
      />
      <div
        className="absolute inset-0"
        style={{
          background:
            'linear-gradient(90deg, #0B2E22 0%, #0B2E22 46%, rgba(11, 46, 34, 0.7) 68%, rgba(11, 46, 34, 0.2) 88%, transparent 100%)'
        }}
        aria-hidden
      />
      <div className="relative z-10 flex min-h-[220px] flex-col justify-end gap-6 px-6 py-8 sm:flex-row sm:items-end sm:justify-between sm:px-8">
        <div className="max-w-2xl">
          {eyebrow ? (
            <p className="text-xs font-semibold uppercase tracking-[0.22em] text-emerald-200">{eyebrow}</p>
          ) : null}
          <h1 className="font-display mt-2 text-3xl font-semibold tracking-tight sm:text-4xl">{title}</h1>
          {description ? <p className="mt-3 max-w-xl text-sm leading-relaxed text-emerald-50 sm:text-base">{description}</p> : null}
        </div>
        {actions ? <div className="flex flex-wrap items-center gap-2">{actions}</div> : null}
      </div>
    </section>
  );
}
