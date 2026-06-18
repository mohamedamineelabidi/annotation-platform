# Gap Analysis — Spec vs. Current Implementation

Mapping of the specification (use cases UC2–UC7) to the existing codebase, and what this build-out adds. Status legend: ✅ done before · 🟡 partial before · 🟕 added in this build-out.

## Entities

| Spec entity | Before | This build-out |
|---|---|---|
| `Dataset` (name, description, classes) | absent (flat `DatasetItem` only) | 🟕 `Dataset` + `DatasetClass`, `DatasetItem.dataset` FK (nullable, back-compatible) |
| `ClassePossible` | global `Label` per `TaskType` | 🟕 `DatasetClass` per dataset, mirrored to `Label` so existing metrics keep working |
| `CoupleTexte` | `DatasetItem(text1,text2)` | ✅ reused |
| `Tâche` (deadline) | `Assignment` (item↔annotator) | 🟕 `Task(dataset, annotator, deadline, active)` |
| `task_items` | implicit in `Assignment` | 🟕 `TaskItem(task, datasetItem, completed)` |
| `Annotation` (classeChoisie) | `Annotation(item,annotator,label)` | ✅ reused; task completion writes an `Annotation` |
| `User` (nom, prenom, login) | username/password/role/enabled | 🟕 added nullable `firstName`/`lastName` |
| `Role` ADMIN/ANNOTATOR | ✅ | ✅ |

## Use cases

| UC | Screen | Before | This build-out |
|---|---|---|---|
| UC2 | Create dataset (Fichier, Nom, Classes `;`, Description) | `/admin/import` (CSV only) | 🟕 `/admin/datasets/new` with classes + description |
| UC3 | Dataset list (Nom, % Avancement, Actions) | — | 🟕 `/admin/datasets` |
| UC3.1 | Dataset details (Taille, %, Classes, couples + pagination, annotateurs) | — | 🟕 `/admin/datasets/{id}` |
| UC3.2/3.3 | Assign annotators (checkboxes + Valider, auto-distribute) | `/admin/assignments` auto-assign | 🟕 `/admin/datasets/{id}/assign` round-robin distribution |
| — | Remove annotator from dataset (keep completed work) | — | 🟕 logical de-assignment (`Task.active=false`, keep `Annotation`s) |
| UC4.1 | List annotators | `/admin/users` | ✅ extended with nom/prénom |
| UC4.2 | Delete annotator (logical) | physical delete | 🟕 logical deactivate (keeps annotations) + reactivate |
| UC4.3 | Modify annotator | ✅ | ✅ |
| UC4.4 | Add annotator (auto password) | manual password | 🟕 auto-generated BCrypt password shown once |
| UC5.1 | Inter-annotator agreement | simple agreement % | 🟕 percent agreement + Cohen's κ + Fleiss' κ + Krippendorff's α |
| UC5.2 | Spammer detection | `SpamDetectionService` | ✅ surfaced on dataset metrics page |
| UC6 | Annotator task list (Id, dataset, deadline, %, taille, Travailler) | `/annotator/dashboard` (flat) | 🟕 `/annotator/tasks` |
| UC7 | Work on task (Text1/Text2, labels, précédent/valider/suivant) | `/annotator/annotate` | 🟕 `/annotator/tasks/{id}/work` scoped to the task |

## Business rules

1. Logical annotator deletion — 🟕 deactivate (`enabled=false`), annotations preserved.
2. Removing annotator from dataset keeps completed work — 🟕 `Task.active=false`, completed `TaskItem`s + `Annotation`s preserved.
3. Dynamic per-dataset classes — 🟕 `DatasetClass`.
4. Automatic task distribution — 🟕 round-robin in `TaskService`.
5. Progress calculation — 🟕 dataset = completed/total task items; task = completed items / assigned items.
6. Admin created manually — ✅ `DataInitializer` seeds admin.
7. Auto-generated, encrypted annotator password — 🟕 random password, BCrypt-encoded.

## Notes
- The original flat flow (`/admin/import`, `/admin/assignments`, `/annotator/annotate`) is left intact for backward compatibility. The new dataset/task flow runs alongside it.
- `DatasetClass` labels are mirrored into `Label` rows (same name + task type) so the existing `Annotation → Label` model, agreement, and spam detection continue to work unchanged.
