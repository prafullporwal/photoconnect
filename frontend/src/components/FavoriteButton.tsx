import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../lib/auth';
import { api } from '../lib/api';
import type { Favorite, FavoriteStatus } from '../lib/types';

/**
 * Heart toggle that saves / unsaves a portfolio item (content) for the
 * logged-in customer. Only renders for customers — anonymous visitors and
 * photographers see nothing.
 *
 * Owns its own status query + optimistic mutation so it works anywhere on
 * the page without prop-drilling. Invalidates ['favorites'] on settle so
 * the saved-content page stays in sync.
 */
export function FavoriteButton({
  portfolioItemId,
  size = 'sm',
  variant = 'overlay',
}: {
  portfolioItemId: string;
  size?: 'sm' | 'lg';
  variant?: 'overlay' | 'inline';
}) {
  const { user } = useAuth();
  const qc = useQueryClient();

  const enabled = user?.role === 'CUSTOMER';

  const statusQuery = useQuery({
    queryKey: ['favorite-status', portfolioItemId],
    queryFn: async () =>
      (await api.get<FavoriteStatus>(`/favorites/${portfolioItemId}/status`)).data,
    enabled,
    staleTime: 30_000,
  });

  const isFavorited = statusQuery.data?.favorited ?? false;

  const toggle = useMutation({
    mutationFn: async (nextState: boolean) => {
      if (nextState) {
        await api.put<Favorite>(`/favorites/${portfolioItemId}`);
      } else {
        await api.delete(`/favorites/${portfolioItemId}`);
      }
    },
    onMutate: async (nextState: boolean) => {
      await qc.cancelQueries({ queryKey: ['favorite-status', portfolioItemId] });
      const previous = qc.getQueryData<FavoriteStatus>(['favorite-status', portfolioItemId]);
      qc.setQueryData<FavoriteStatus>(['favorite-status', portfolioItemId], { favorited: nextState });
      return { previous };
    },
    onError: (_err, _next, context) => {
      if (context?.previous) {
        qc.setQueryData(['favorite-status', portfolioItemId], context.previous);
      }
    },
    onSettled: () => {
      qc.invalidateQueries({ queryKey: ['favorite-status', portfolioItemId] });
      qc.invalidateQueries({ queryKey: ['favorites'] });
    },
  });

  if (!enabled) return null;

  const handleClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    toggle.mutate(!isFavorited);
  };

  const label = isFavorited ? 'Remove from saved' : 'Save this';

  if (variant === 'inline') {
    return (
      <button
        type="button"
        onClick={handleClick}
        aria-pressed={isFavorited}
        aria-label={label}
        disabled={toggle.isPending && !statusQuery.data}
        className={`inline-flex items-center gap-1.5 rounded-full border px-4 py-2 text-sm font-semibold transition ${
          isFavorited
            ? 'border-rose-200 bg-rose-50 text-rose-600 hover:bg-rose-100'
            : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
        }`}
      >
        <HeartIcon filled={isFavorited} className={size === 'lg' ? 'h-5 w-5' : 'h-4 w-4'} />
        {isFavorited ? 'Saved' : 'Save'}
      </button>
    );
  }

  return (
    <button
      type="button"
      onClick={handleClick}
      aria-pressed={isFavorited}
      aria-label={label}
      title={label}
      className={`grid place-items-center rounded-full backdrop-blur-md transition ${
        size === 'lg' ? 'h-10 w-10' : 'h-8 w-8'
      } ${
        isFavorited
          ? 'bg-rose-500/90 text-white shadow-md hover:bg-rose-500'
          : 'bg-black/45 text-white/90 hover:bg-black/65 hover:text-white'
      }`}
    >
      <HeartIcon filled={isFavorited} className={size === 'lg' ? 'h-5 w-5' : 'h-4 w-4'} />
    </button>
  );
}

function HeartIcon({ filled, className }: { filled: boolean; className?: string }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill={filled ? 'currentColor' : 'none'}
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      className={className}
    >
      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
    </svg>
  );
}
