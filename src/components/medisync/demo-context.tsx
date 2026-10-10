import { createContext, useContext, useState, type ReactNode } from 'react';
import { initialBookings, type Booking } from '@/lib/demo-data';
type DemoState = { bookings: Booking[]; addBooking: (booking: Omit<Booking, 'id'>) => void; removeBooking: (id: string) => void; completed: string[]; toggleEncounter: (id: string) => void; tenants: number; seats: number; setTenants: (value: number) => void; setSeats: (value: number) => void };
const DemoContext = createContext<DemoState | undefined>(undefined);
export function DemoProvider({ children }: { children: ReactNode }) {
  const [bookings, setBookings] = useState(initialBookings);
  const [completed, setCompleted] = useState<string[]>([]);
  const [tenants, setTenants] = useState(3);
  const [seats, setSeats] = useState(5);
  function addBooking(booking: Omit<Booking, 'id'>) { setBookings(current => current.some(b => b.providerId === booking.providerId && b.time === booking.time && b.day === booking.day) ? current : [...current, { ...booking, id: crypto.randomUUID() }]); }
  function toggleEncounter(id: string) { setCompleted(current => current.includes(id) ? current.filter(item => item !== id) : [...current, id]); }
  return <DemoContext.Provider value={{ bookings, addBooking, removeBooking: id => setBookings(current => current.filter(b => b.id !== id)), completed, toggleEncounter, tenants, seats, setTenants, setSeats }}>{children}</DemoContext.Provider>;
}
export function useDemo() { const context = useContext(DemoContext); if (!context) throw new Error('DemoProvider is required'); return context; }