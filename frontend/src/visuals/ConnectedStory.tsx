import { PHOTOS } from '../media/photos';
import { SignalBackdrop } from './SignalBackdrop';

type Variant = 'orbit' | 'devices' | 'field' | 'action';

const SETS: Record<Variant, { photo: (typeof PHOTOS)[keyof typeof PHOTOS]; label: string; line: string }> = {
  orbit: {
    photo: PHOTOS.satellite,
    label: 'Orbit',
    line: 'Earth-observation for vegetation, rainfall, and heat stress.'
  },
  devices: {
    photo: PHOTOS.devicesTeam,
    label: 'Devices',
    line: 'Operators, aggregators, and field agents on one live console.'
  },
  field: {
    photo: PHOTOS.farmerField,
    label: 'Field',
    line: 'Plots, crops, and harvest on the record partners already use.'
  },
  action: {
    photo: PHOTOS.fieldsAerial,
    label: 'Mandate',
    line: 'Decide from the current — not after the loss is reconstructed.'
  }
};

export function ConnectedStory({ variant, className = '' }: { variant: Variant; className?: string }) {
  const item = SETS[variant];
  return (
    <SignalBackdrop photo={item.photo} kenBurns className={`min-h-[280px] rounded-3xl sm:min-h-[340px] ${className}`}>
      <div className="flex min-h-[280px] flex-col justify-end px-6 py-8 text-white sm:min-h-[340px] sm:px-8">
        <p className="text-xs font-semibold uppercase tracking-[0.22em] text-emerald-200">{item.label}</p>
        <p className="mt-2 max-w-sm text-lg font-semibold leading-snug">{item.line}</p>
      </div>
    </SignalBackdrop>
  );
}
