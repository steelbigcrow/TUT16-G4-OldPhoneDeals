import type { ReactNode } from 'react'
import { Link, Outlet, useParams } from 'react-router-dom'

function PageShell(props: { title: string; children?: ReactNode }) {
  return (
    <div style={{ padding: 16 }}>
      <h1 style={{ fontSize: 20, fontWeight: 600 }}>{props.title}</h1>
      <div style={{ marginTop: 12 }}>{props.children}</div>
    </div>
  )
}

export function HomePage() {
  return (
    <PageShell title='Home'>
      <p>占位页面：/home</p>
      <ul>
        <li>
          <Link to='/search'>/search</Link>
        </li>
        <li>
          <Link to='/phone/123'>/phone/:id</Link>
        </li>
        <li>
          <Link to='/login'>/login</Link>
        </li>
        <li>
          <Link to='/admin/login'>/admin/login</Link>
        </li>
        <li>
          <Link to='/checkout'>/checkout (protected user)</Link>
        </li>
        <li>
          <Link to='/admin'>/admin (protected admin)</Link>
        </li>
      </ul>
    </PageShell>
  )
}

export function SearchPage() {
  return (
    <PageShell title='Search'>
      <p>占位页面：/search</p>
    </PageShell>
  )
}

export function PhoneDetailPage() {
  const params = useParams()
  return (
    <PageShell title='Phone Detail'>
      <p>占位页面：/phone/:id</p>
      <p>id: {params.id}</p>
    </PageShell>
  )
}

export function LoginPage() {
  return (
    <PageShell title='Login'>
      <p>占位页面：/login</p>
      <p>仅占位，不实现表单。</p>
    </PageShell>
  )
}

export function RegisterPage() {
  return (
    <PageShell title='Register'>
      <p>占位页面：/register</p>
      <p>仅占位，不实现表单。</p>
    </PageShell>
  )
}

export function VerifyEmailPage() {
  return (
    <PageShell title='Verify Email'>
      <p>占位页面：/verify-email</p>
    </PageShell>
  )
}

export function ResetPasswordPage() {
  return (
    <PageShell title='Reset Password'>
      <p>占位页面：/reset-password</p>
      <p>仅占位，不实现表单。</p>
    </PageShell>
  )
}

export function CheckoutPage() {
  return (
    <PageShell title='Checkout'>
      <p>占位页面：/checkout (user protected)</p>
    </PageShell>
  )
}

export function WishlistPage() {
  return (
    <PageShell title='Wishlist'>
      <p>占位页面：/wishlist (user protected)</p>
    </PageShell>
  )
}

export function ProfileLayoutPage() {
  return (
    <PageShell title='Profile'>
      <ul>
        <li>
          <Link to='/profile'>/profile</Link>
        </li>
        <li>
          <Link to='/profile/settings'>/profile/settings</Link>
        </li>
      </ul>
      <div style={{ marginTop: 12, padding: 12, border: '1px solid #e5e7eb' }}>
        <Outlet />
      </div>
    </PageShell>
  )
}

export function ProfileHomePage() {
  return (
    <PageShell title='Profile Home'>
      <p>占位页面：/profile</p>
    </PageShell>
  )
}

export function ProfileSettingsPage() {
  return (
    <PageShell title='Profile Settings'>
      <p>占位页面：/profile/settings</p>
    </PageShell>
  )
}

export function AdminLoginPage() {
  return (
    <PageShell title='Admin Login'>
      <p>占位页面：/admin/login</p>
      <p>仅占位，不实现表单。</p>
    </PageShell>
  )
}

export function AdminLayoutPage() {
  return (
    <PageShell title='Admin'>
      <ul>
        <li>
          <Link to='/admin'>/admin</Link>
        </li>
        <li>
          <Link to='/admin/dashboard'>/admin/dashboard</Link>
        </li>
        <li>
          <Link to='/admin/users'>/admin/users</Link>
        </li>
      </ul>
      <div style={{ marginTop: 12, padding: 12, border: '1px solid #e5e7eb' }}>
        <Outlet />
      </div>
    </PageShell>
  )
}

export function AdminHomePage() {
  return (
    <PageShell title='Admin Home'>
      <p>占位页面：/admin</p>
    </PageShell>
  )
}

export function AdminDashboardPage() {
  return (
    <PageShell title='Admin Dashboard'>
      <p>占位页面：/admin/dashboard</p>
    </PageShell>
  )
}

export function AdminUsersPage() {
  return (
    <PageShell title='Admin Users'>
      <p>占位页面：/admin/users</p>
    </PageShell>
  )
}

export function NotFoundPage() {
  return (
    <PageShell title='404'>
      <p>Not Found</p>
      <p>
        <Link to='/home'>返回 /home</Link>
      </p>
    </PageShell>
  )
}