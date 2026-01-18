---
name: react-typescript
description: React 19 + TypeScript patterns, hooks, types, and best practices
license: MIT
compatibility: opencode
metadata:
  audience: frontend-developers
  frameworks: react typescript
---

## What I do
- Create type-safe React components with TypeScript
- Use React hooks correctly (useState, useEffect, useMemo, useRef)
- Type props, state, and event handlers
- Optimize re-renders with memoization and keys
- Implement context providers and custom hooks
- Handle forms, async operations, and error boundaries

## When to use me
Use me when building React + TypeScript applications, especially when:
- Creating new components or refactoring existing ones
- Adding types to props and state
- Implementing complex hooks or context patterns
- Optimizing performance (memo, useMemo, useCallback)
- Handling forms and user input
- Debugging TypeScript errors

## Component patterns
```tsx
interface Props {
  title: string;
  count: number;
  onIncrement?: () => void;
}

export function Counter({ title, count, onIncrement }: Props) {
  return (
    <div>
      <h1>{title}</h1>
      <p>Count: {count}</p>
      <button onClick={onIncrement}>Increment</button>
    </div>
  );
}
```

## Hook usage patterns
- `useState<T>(initialValue)` - Type state explicitly if inferred
- `useEffect(() => { ... }, [deps])` - Include all dependencies
- `useMemo(() => expensive(a, b), [a, b])` - Memoize expensive values
- `useCallback(() => action(a, b), [a, b])` - Memoize callbacks
- `useRef<T>(null)` - Type refs with generic
- `useContext(Context)` - Type context with Context type

## Type definitions
```tsx
// Props with optional and union types
interface ButtonProps {
  variant: 'primary' | 'secondary';
  disabled?: boolean;
  onClick: (event: React.MouseEvent<HTMLButtonElement>) => void;
}

// Generic components
interface ListProps<T> {
  items: T[];
  render: (item: T) => React.ReactNode;
}
function List<T>({ items, render }: ListProps<T>) {
  return <ul>{items.map(render)}</ul>;
}
```

## Event handling
```tsx
// Form events
handleSubmit(event: React.FormEvent<HTMLFormElement>) {
  event.preventDefault();
  // Access form: event.currentTarget.elements.name.value
}

// Input events
handleChange(event: React.ChangeEvent<HTMLInputElement>) {
  setValue(event.target.value);
}

// Mouse events
handleClick(event: React.MouseEvent<HTMLButtonElement>) {
  console.log(event.clientX, event.clientY);
}
```

## Performance optimization
- Use `React.memo` for components that shouldn't re-render
- Memoize with `useMemo` for expensive calculations
- Memoize callbacks with `useCallback` for child props
- Provide stable keys for lists (use IDs, not indexes)
- Avoid creating new objects/arrays in render

## Context patterns
```tsx
// Create typed context
interface AppContext {
  user: User | null;
  login: (email: string) => Promise<void>;
}
const AppContext = createContext<AppContext | null>(null);

// Provider component
function AppProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const login = useCallback(async (email: string) => {
    // ...
  }, []);
  return (
    <AppContext.Provider value={{ user, login }}>
      {children}
    </AppContext.Provider>
  );
}

// Custom hook to use context
function useApp() {
  const context = useContext(AppContext);
  if (!context) throw new Error('useApp must be used within AppProvider');
  return context;
}
```

## Error boundaries
```tsx
interface ErrorBoundaryProps {
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

class ErrorBoundary extends Component<ErrorBoundaryProps, { error: Error | null }> {
  state = { error: null };
  static getDerivedStateFromError(error: Error) {
    return { error };
  }
  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Error caught:', error, errorInfo);
  }
  render() {
    if (this.state.error) {
      return this.props.fallback || <div>Something went wrong</div>;
    }
    return this.props.children;
  }
}
```

## Form handling
- Use controlled components with state
- Type form data with interfaces
- Validate before submission
- Handle loading and error states
- Use `useForm` from libraries (react-hook-form) for complex forms

## Strict mode
- Always use `<React.StrictMode>` in development
- Detects side effects and unsafe lifecycles
- Invokes effects twice in dev to catch bugs
- Enables React DevTools profiling

## TypeScript strict mode
- Enable `"strict": true` in tsconfig.json
- Use `unknown` instead of `any` for dynamic data
- Type API responses with interfaces
- Use discriminated unions for state machines
