import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { HeadContent, Outlet, Link, createRootRouteWithContext } from '@tanstack/react-router';
import { AppShell } from '@/components/medisync/shell';
import { DemoProvider } from '@/components/medisync/demo-context';
export const Route = createRootRouteWithContext<{ queryClient: QueryClient }>()({
  head: () => ({ meta: [{ title: 'MediSyncD Telehealth · Fictional Demo' },{ name:'description', content:'MediSyncD Technologies, LLC telehealth product demonstration. No real appointments or clinical services.' }] }),
  component: RootLayout,
  notFoundComponent: () => <main style={{padding:'3rem',textAlign:'center'}}><h1>Page not found</h1><Link to="/">Return to overview</Link></main>,
});
function RootLayout(){
  const { queryClient } = Route.useRouteContext();
  return <QueryClientProvider client={queryClient}><HeadContent /><DemoProvider><AppShell><Outlet /></AppShell></DemoProvider></QueryClientProvider>
}
