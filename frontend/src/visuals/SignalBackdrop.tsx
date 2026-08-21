import { PHOTOS } from '../media/photos';
import { PhotoStage } from './PhotoStage';

type Photo = (typeof PHOTOS)[keyof typeof PHOTOS];

type Props = {
  children?: React.ReactNode;
  photo?: Photo;
  className?: string;
  kenBurns?: boolean;
  /** `panel` is a longer, softer fade for narrow columns such as login. */
  blend?: 'hero' | 'panel';
};

const BLEND = {
  hero:
    'linear-gradient(90deg, #0B2E22 0%, rgba(11, 46, 34, 0.94) 28%, rgba(11, 46, 34, 0.72) 48%, rgba(11, 46, 34, 0.38) 68%, rgba(11, 46, 34, 0.12) 84%, transparent 100%)',
  panel:
    'linear-gradient(90deg, #0B2E22 0%, #0B2E22 58%, rgba(11, 46, 34, 0.88) 74%, rgba(11, 46, 34, 0.55) 88%, rgba(11, 46, 34, 0.28) 100%)'
};

export function SignalBackdrop({
  children,
  photo = PHOTOS.fieldsAerial,
  className = 'min-h-[72vh]',
  kenBurns = true,
  blend = 'hero'
}: Props) {
  return (
    <div className={`relative overflow-hidden bg-sidebar ${className}`}>
      <PhotoStage
        src={photo.src}
        alt=""
        kenBurns={kenBurns}
        priority
        className="absolute inset-0"
        imgClassName="object-[78%_center]"
      />
      <div className="absolute inset-0" style={{ background: BLEND[blend] }} aria-hidden />
      <div className="relative z-10">{children}</div>
    </div>
  );
}
