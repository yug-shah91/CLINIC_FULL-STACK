# Clinic Frontend

React + Vite single-page app for the Clinic API. See the README one folder up for how to run everything together.

**Stack:** React 18, Vite, React Router 6, Axios, lucide-react (icons), jsPDF (prescription PDFs), plain CSS.

```bash
npm install
npm run dev      # http://localhost:5173, forwards /api to http://localhost:8080
npm run build    # writes the website into ../backend/src/main/resources/static
```

## Structure

```
src/
  api/          axios.js (one client, interceptors) and services.js (one function per endpoint)
  context/      AuthContext (who is logged in), ToastContext (popup messages)
  hooks/        useFetch (load data), useDebounce (search box), useAppointmentActions (confirm/complete/cancel)
  components/   Layout (sidebar and mobile tab bar), SlotPicker, Chart, Timeline, Skeleton, StatCard, Modal, ...
  pages/
    patient/    Profile, BookAppointment, MyAppointments
    admin/      Dashboard, AllAppointments, Patients, PatientDetail, Prescription, Reports
  utils/        format.js (dates, labels), validators.js, prescriptionPdf.js
  styles/       index.css (colours, spacing and type scale at the top)
  App.jsx       The route table
```

## Ideas worth knowing

- **Loading skeletons** (`Skeleton.jsx`) keep the layout stable while data loads. Every list has a loading, error and empty state.
- **`useFetch(fetcher, deps)`** re-runs whenever a value in `deps` changes. Filters and pagination are just state that sits in that list.
- **Charts** are drawn as plain SVG in `Chart.jsx`, so there is no chart library to learn or update.
- **Route protection:** `ProtectedRoute` sends logged-out users to `/login` and wrong-role users home. The real security is in Spring.
- **Responsive:** below 860px the sidebar becomes a bottom tab bar and the doctor's tables become stacked cards.

## Deploying separately

Set `VITE_API_URL` (see `.env.example`) and enable CORS in Spring Security for the frontend's origin.
