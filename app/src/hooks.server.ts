// If this file has no other server-specific logic, clear its content or delete it.
// If there is other logic, keep only that.

/** @type {import('@sveltejs/kit').Handle} */
export async function handle({ event, resolve }) {
  // Remove all Tauri API related code.
  const response = await resolve(event)
  return response
}
