# Space Invaders

> Course: Software Development Practices (CES2024) - 25-2-24788

This course project provides an expanded gameplay experience for **Space Invaders**, including enhancements and revisions across maps, bosses, items, and other core systems. The project is developed following **Agile Kanban principles**, with a continuous flow of iterative feature improvements.

## 👩‍💻 New Features

### 1. Map & Story
- Added a **MapScreen** that represents each level.
- On the map, interactions are done through the **Next** button:
  - **Esc**: Return to the TitleScreen
  - **Space**: Play the selected level
- Clearing a level unlocks the next one.
- A portion of the game’s story is displayed at the start of every level.

### 2. Boss
- After clearing Level 5, a new **boss level** becomes available.
- The boss level has **two phases**:
  - **Phase 1:** The boss fires bullets in three directions, and the player must defeat **10 small enemies** first.
  - **Phase 2:** Triggered when the boss’s HP falls below **50%**. The boss fires bullets in four directions, and the player must defeat **15 small enemies** first.
- **New Ranking System:** A dedicated ranking applies to the boss level. A timer records how quickly the player defeats the boss, and rankings are assigned in order of fastest completion.

### 3. Revive & Items
- Players can now **revive once per stage** by spending **50 coins**, restoring 1 life.
- Items are **used immediately upon pickup**.
  - If a previously collected item is still active, newly acquired items cannot be used.
  - Up to **two item effects** can be visualized and active in the inventory while the player is attacking.
  - Picking up an item that already exists in the inventory **extends its active duration**.

## 🥸 Team & Roles

### Management & Coordination
**Project Manager(PM)**: Manages the Jira Kanban board and the overall schedule.
- `Hamin Park`, `Inhwa Park`

**Product Owner(PO)**: Manages the Jira backlog.
- `Dahye Jung`, `Dain Jeong`

**Integration Manager(IM)**: Oversees Git, CI, and code quality.
- `Yerim Kim`, `Inhwa Park`

**Quality Assurance(QA)**: Identifies bugs and ensures code quality.
- `Sumin Seak`, `Seohyun Park`


### Developement 
| System / Module | Responsibility                                  | member |
| --------------- | ----------------------------------------------- | --- |
| **Map**         | Develops the map UI and level navigation logic  |[Yerim Kim](https://github.com/doorimng), [Dahye Jung](https://github.com/dahye011)
| **Story**       | Plans and integrates the game story             |[Sumin Seak](https://github.com/245387)
| **Boss**        | Implements and manages the new boss content     |[Inhwa Park](https://github.com/duckduckhwa), [Dain Jeong](https://github.com/manyperson)
| **Item**        | Manages item acquisition and usage systems      |[Hamin Park](https://github.com/pkhamin)
| **Revive**      | Handles the revive system and related mechanics |[Seohyun Park](https://github.com/11223344eeeee-source)


## ✔️ CI Configuration
### Local Testing

- `JUnit`: Used for writing unit tests

- `JaCoCo`: Generates reports and measures test coverage

### Automation / Deployment

- `SonarQubeCloud` + `GitHub Actions`: Handles code quality analysis, automated builds, and continuous deployment

## 🌿 Branch Strategy

Branch naming convention: `<prefix>/<kanban-number>-<feature-name>`


| Branch Name / Prefix | Purpose |
| :------------------- | :------ | 
| `master`             | Main branch for deployment      
| `feature/`           |  Feature development branches 
| `test/`              | Testing branches                         
| `fix/`               | Bug fix branches                          
| `ci/`                |  CI configuration branches          


## Development

- IDE : IntelliJ IDEA

## System Requirements

Requires Java 7 or better.

### Resources

Note that you should install the following resources in order to run the game.


- [Space Invaders Regular (font)](http://www.fonts2u.com/space-invaders-regular.font) - &copy; kylemaoin 2010
