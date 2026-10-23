# Return Gate Design System

## Visual thesis

**Premium airport wayfinding, expressed as a calm digital recovery desk.** The interface uses strong type, careful alignment, and route-inspired detail to make a stressful task feel clear and manageable.

## Typography

- Exclusively San Francisco (Apple native system stack: `-apple-system, BlinkMacSystemFont, "SF Pro Text", "SF Pro", system-ui, sans-serif`) and Montserrat.
- Headings, wordmarks, navigation, eyebrows, counters, and button labels: Montserrat, used at deliberate sizes and weights.
- Body, controls, notices, descriptions, and input elements: San Francisco system stack.
- Avoid excessively tight tracking in body copy; body text starts around 14–17px.
- No other font families are loaded or referenced.

## Palette tokens

- Page: `#f4f6f8`
- White surface: `#ffffff`
- Terminal navy: `#102c3d`
- Ink: `#132d3c`
- Atlantic teal: `#087681`
- Teal hover: `#065f68`
- Teal pale: `#e0f1f0`
- Amber waypoint: `#dfad50`
- Primary border: `#dbe4e9`
- Muted text: `#667b88`

Teal marks primary actions and selected states. Amber is reserved for connection/status details. Navy provides structure and contrast. Surfaces and borders establish grouping without wrapping every object in a card.

## Layout and components

- Sticky desktop header with a restrained responsive mobile menu.
- Editorial two-column hero: product message at left and a route illustration panel at right.
- Search surface with category chips and a dedicated honest empty state.
- Empty states share an illustration motif and accessible status language.
- Report and sign-in dialogs use Radix primitives, with custom styling and visible focus states.
- Forms are grouped by item attributes and journey details.

## Data integrity

This package includes no mock reports, example passenger accounts, fake match confidence values, or fabricated service status. The user interface explicitly states that the data/authentication service is not connected. Report inputs are never submitted or persisted.

## Responsive and accessibility principles

- Desktop, tablet, and mobile layouts use content-driven breakpoints.
- Buttons and fields retain practical touch sizes.
- Semantic elements, visible `:focus-visible`, labelled inputs, skip navigation, and reduced-motion rules are included.
- Empty/error/service-unavailable states use explanatory text and are not communicated by colour alone.

## Anti-patterns avoided

- No fabricated dashboard metrics or matching scores.
- No seeded reports used as fake search results.
- No demo passenger that signs in without authentication.
- No large gradient-orb hero, arbitrary floating decoration, or indiscriminate UI framework mixing.
- No false success message for a form that cannot reach the backend.
