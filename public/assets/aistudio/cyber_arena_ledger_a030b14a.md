---
name: Cyber Arena & Ledger
colors:
  surface: '#121416'
  surface-dim: '#121416'
  surface-bright: '#38393c'
  surface-container-lowest: '#0c0e10'
  surface-container-low: '#1a1c1e'
  surface-container: '#1e2022'
  surface-container-high: '#282a2c'
  surface-container-highest: '#333537'
  on-surface: '#e2e2e5'
  on-surface-variant: '#c2c9b3'
  inverse-surface: '#e2e2e5'
  inverse-on-surface: '#2f3033'
  outline: '#8d937f'
  outline-variant: '#434938'
  surface-tint: '#9ed753'
  primary: '#fdfff2'
  on-primary: '#203700'
  primary-container: '#b8f36b'
  on-primary-container: '#456f00'
  inverse-primary: '#416900'
  secondary: '#c2c7cc'
  on-secondary: '#2c3135'
  secondary-container: '#42474c'
  on-secondary-container: '#b1b6bb'
  tertiary: '#fcfff9'
  on-tertiary: '#00391f'
  tertiary-container: '#8cf7b5'
  on-tertiary-container: '#007343'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#b9f46c'
  primary-fixed-dim: '#9ed753'
  on-primary-fixed: '#102000'
  on-primary-fixed-variant: '#304f00'
  secondary-fixed: '#dfe3e8'
  secondary-fixed-dim: '#c2c7cc'
  on-secondary-fixed: '#171c20'
  on-secondary-fixed-variant: '#42474c'
  tertiary-fixed: '#8ef8b6'
  tertiary-fixed-dim: '#71db9c'
  on-tertiary-fixed: '#002110'
  on-tertiary-fixed-variant: '#00522f'
  background: '#121416'
  on-background: '#e2e2e5'
  surface-variant: '#333537'
typography:
  headline-xl:
    fontFamily: Space Grotesk
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.03em
  headline-xl-mobile:
    fontFamily: Space Grotesk
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Space Grotesk
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 30px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Space Grotesk
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 26px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Space Grotesk
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 22px
    letterSpacing: 0em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: JetBrains Mono
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 18px
    letterSpacing: 0.02em
  label-md:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.04em
  label-sm:
    fontFamily: JetBrains Mono
    fontSize: 10px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.06em
  currency-display:
    fontFamily: JetBrains Mono
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 28px
    letterSpacing: -0.02em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 0.75rem
  margin: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system defines an elite, high-performance competitive mobile environment tailored for the Bangladeshi esports ecosystem. It fuses the surgical precision and trust of modern fintech with the kinetic energy of tier-one competitive gaming. The interface rejects unnecessary gaming clichés—such as excessive skeuomorphic chrome or chaotic RGB gradients—in favor of an ultra-clean, dark-first architecture prioritizing rapid decision-making, clear prize distribution, and tournament integrity.

The brand persona is disciplined, razor-sharp, and authoritative. It evokes focus, professional legitimacy, and intense momentum. By grounding dynamic tournament data in structured, fintech-grade layouts, the system elevates grassroots and national esports competitions to a world-class standard.

## Colors

The palette establishes strict spatial hierarchy through deep obsidian tiers, ignited by surgical flashes of high-voltage electric lime.

- **Primary Accent (`#B8F36B`)**: Electric lime green. Reserved exclusively for primary actions, active indicator states, winning bracket nodes, and focal metrics. It is never used for broad background fills.
- **Surface Foundation (`#0B0D0F`)**: Canvas backdrop. Serves as the bedrock of the app experience.
- **Secondary Surface (`#121619`)**: Grouping containers, navigation frames, and bottom app bar backgrounds.
- **Card Background (`#171C20`)**: Elevated cards, list modules, and tactical data panels.
- **Border & Dividers (`#252C2E`)**: Crisp structural lines with precise 1px definition.
- **Text & Foreground**:
  - Primary (`#F5F7F5`): High-clarity off-white for critical stats, titles, and team handles.
  - Secondary (`#929B99`): Muted industrial slate for meta information, rules, timestamps, and subtitles.
- **Functional Semantics**:
  - Danger / Broadcast Live (`#EF6464`): Match losses, error states, and live broadcast pings.
  - Success (`#65CF91`): Wallet credits, confirmed tournament registrations, and verified verifications.

## Typography

Typography anchors the dual identity of competitive esports and financial ledger accuracy.

- **Space Grotesk** commands display titles, section banners, and tournament names with sharp, geometric edge-cuts.
- **Inter** ensures absolute readability across participant rosters, rule engines, match schedules, and system dialogs.
- **JetBrains Mono** powers all ledger items, countdown timers, tournament seeds, match kill/death ratios, and monetary balances using the Bangladeshi Taka symbol (`৳`). Tabular figures prevent layout shifts during live score and balance updates.

## Layout & Spacing

The layout is built for high-density, one-handed mobile navigation with an uncompromising 4px spatial baseline.

- **Mobile Viewport Structure**: Standard screen margins are pinned at `1rem` (16px), expanding gutters to `0.75rem` (12px) for nested modular grids like participant leaderboards and match listings.
- **Vertical Cadence**: Component stacks leverage `space-sm` (8px) for tightly coupled meta tags and `space-md` (16px) for stacked match cards. Primary screen sections are spaced via `space-xl` (32px).
- **Safe Area Adaptations**: Fixed navigation and sticky bottom action bars integrate platform-native bottom-inset spacing plus an absolute minimum `1rem` padding from interactive zones.

## Elevation & Depth

Visual hierarchy relies on structural tonal layering, crisp borders, and subtle tactical ambient lighting rather than heavy drop shadows.

- **Level 0 (Base Canvas)**: `#0B0D0F`.
- **Level 1 (Structural Panels & Navbars)**: `#121619` with a subtle 1px border stroke of `#252C2E`.
- **Level 2 (Active Cards & Modals)**: `#171C20` elevated with a 1px border of `#252C2E`. When focused or live, borders shift to an electric tint (`rgba(184, 243, 107, 0.4)`).
- **Tactical Underglow**: Critical active elements (such as "Join Tournament" CTAs or live match indicators) feature an ambient backlight: `0px 4px 20px rgba(184, 243, 107, 0.15)`.
- **Match Alerts**: Pulsing status indicators use a localized aura: `0px 0px 12px rgba(239, 100, 100, 0.4)`.

## Shapes

The geometric framework balances technical rigor with modern tactical ergonomics.

- **Standard Containers & Cards**: Set to `0.5rem` (8px) for a structured, modular feel across cards and match blocks.
- **Pill Badges & Selectors**: Chip selectors, game category tags, and status badges utilize complete pill radii (`9999px`) to visually isolate dynamic metadata from square interface blocks.
- **Micro Accents**: Diagonal tick marks and 45-degree chamfers are subtly applied to secondary badge corners and bracket connector anchors to evoke cybernetic and mechanical instrumentation.

## Components

### Buttons
- **Primary Action**: Solid `#B8F36B` surface with `#0B0D0F` bold typography (`label-lg`). Minimum touch height of 48px. Subtle green glow on active state.
- **Secondary Action**: `#171C20` surface with 1px `#252C2E` outline and `#F5F7F5` typography. Hover/press transitions border to `#B8F36B`.
- **Destructive/Live Action**: Transparent surface with `#EF6464` stroke and matching text color.

### Tournament Cards
- Background: `#171C20`.
- Border: 1px `#252C2E`.
- Header: Game banner thumbnail with gradient scrim to transparent, crowned with a game-type pill badge.
- Body: Title in `Space Grotesk`, match format, entry fee, and total prize pool anchored by `JetBrains Mono` (`৳ [Amount]`).
- Progress: High-contrast thin meter bar tracking registered slots (e.g., 64/100 Teams) using `#B8F36B` against `#252C2E`.

### Bracket Nodes
- Dual-cell vertical stacks representing opposing teams.
- Winner row highlighted with `#121619` surface, 1px `#B8F36B` edge accent, and bright white text. Loser row rendered with 50% opacity.
- Dynamic connector paths drawn using 1.5px `#252C2E` strokes, illuminating in `#B8F36B` for active path progression.

### Fintech Wallet Card
- Surface: Multi-stop angled dark gradient (`#171C20` to `#121619`) with a 1px border of `#252C2E`.
- Data Hierarchy: Available balance in `currency-display` with the prefix `৳`, subtext displaying locked escrow stakes, and quick-action utility pills (`Deposit`, `Withdraw`) inline.

### Custom Tab Switches & Segmented Controls
- Background frame: `#0B0D0F` with 4px inner padding.
- Tab Pill: Active segment takes `#171C20` with `#F5F7F5` text and 1px border `#252C2E`; inactive segments show `#929B99` without background fill.

### Bottom Navigation Bar
- Fixed bottom docked bar: `#121619` with a top 1px border `#252C2E` and `backdrop-filter: blur(12px)`.
- Active item uses an electric lime icon alongside a micro glowing dot indicator below the label. Inactive items rest at `#929B99`.