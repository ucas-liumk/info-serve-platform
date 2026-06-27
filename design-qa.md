# Portal Home Visual QA

final result: passed

## Scope

- Target: `/portal` information center homepage.
- Reference: user-provided light homepage screenshot in the current task.
- Implementation screenshot: `output/portal-visual/portal-home-iconify-arrow-alt.png`.

## Checks

- Desktop viewport matched to the reference size: 1672 x 941.
- Top-left brand lockup, title scale, slogan spacing, and top-right translucent status panel match the reference direction.
- Great Wall subject remains visible above and between the module cards, instead of being fully covered by the card row.
- Five module cards are present with large 3D-style visuals, matching titles, matching subtitles, circular action buttons, and consistent spacing.
- Module action buttons use a standard Iconify component (`material-symbols/arrow-right-alt-rounded`) rather than hand-drawn CSS or temporary image assets.
- Module action buttons keep a fixed 1:1 round shape; they no longer compress into flat pills.
- Bottom service overview band matches the reference structure with title block, six metric groups, round icons, vertical dividers, and live service data.
- The first-phase active entrances still work: resource sharing, tool center, and service forum all navigate from the new homepage.
- Responsive behavior is covered with tablet and mobile breakpoints so content stacks without overlap.

## Notes

- The implementation intentionally uses live date/time and current service statistics rather than the static numbers shown in the reference image.
- The generated Great Wall background is a clean project asset without embedded UI text, so the homepage remains real HTML/CSS instead of a flattened screenshot.
