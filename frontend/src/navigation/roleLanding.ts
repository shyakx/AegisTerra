/** Default post-login landing path — persona dashboards live on `/app`. */
export function roleLandingPath(roles: string[] | undefined): string {
  void roles;
  return '/app';
}
