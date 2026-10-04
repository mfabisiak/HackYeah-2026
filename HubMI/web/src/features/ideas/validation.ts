import { MAX_IDEA_ESSENCE_LENGTH, MAX_IDEA_TITLE_LENGTH } from './constants'
import type { IdeaDraft } from './localData'

export type IdeaField = 'title' | 'essence' | 'targetGroups' | 'stage'

export type IdeaErrors = Partial<Record<IdeaField, string>>

/** Mirrors IdeaDraft.parse on the server. */
export function validateIdea(idea: IdeaDraft): IdeaErrors {
  const title = idea.title.trim()
  const essence = idea.essence.trim()
  return {
    ...(title === ''
      ? { title: 'Wpisz tytuł pomysłu' }
      : title.length > MAX_IDEA_TITLE_LENGTH
        ? { title: `Tytuł nie może być dłuższy niż ${MAX_IDEA_TITLE_LENGTH} znaków` }
        : {}),
    ...(essence === ''
      ? { essence: 'Opisz, na czym polega pomysł' }
      : essence.length > MAX_IDEA_ESSENCE_LENGTH
        ? { essence: `Opis nie może być dłuższy niż ${MAX_IDEA_ESSENCE_LENGTH} znaków` }
        : {}),
    ...(idea.targetGroups.length === 0 ? { targetGroups: 'Zaznacz co najmniej jedną grupę odbiorców' } : {}),
    ...(idea.stage === '' ? { stage: 'Wybierz etap, na którym jest pomysł' } : {}),
  }
}

/** Which fields the step is responsible for (steps: 0 essence, 1 audience, 2 stage, 3 summary). */
export const STEP_FIELDS: readonly (readonly IdeaField[])[] = [['title', 'essence'], ['targetGroups'], ['stage'], []]

export const IDEA_FIELD_IDS: Record<IdeaField, string> = {
  title: 'idea-title',
  essence: 'idea-essence',
  targetGroups: 'idea-target-groups',
  stage: 'idea-stage',
}
