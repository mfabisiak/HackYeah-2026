import type { AssistIdea } from './useAssistStream'

export const MAX_TITLE_LENGTH = 200
export const MAX_ESSENCE_LENGTH = 1000

export interface IdeaValues {
  title: string
  essence: string
  targetGroups: string[]
  stage: string | null
}

export type IdeaErrors = Partial<Record<'title' | 'essence' | 'targetGroups' | 'stage', string>>

/** The same rules as the server's `IdeaDraft`, so that what is rejected here would be rejected there. */
export function validateIdea(values: IdeaValues): { errors: IdeaErrors; idea: AssistIdea | null } {
  const title = values.title.trim()
  const essence = values.essence.trim()
  const errors: IdeaErrors = {
    ...(title === '' ? { title: 'Wpisz tytuł pomysłu.' } : {}),
    ...(title.length > MAX_TITLE_LENGTH ? { title: `Tytuł może mieć najwyżej ${MAX_TITLE_LENGTH} znaków.` } : {}),
    ...(essence === '' ? { essence: 'Opisz w kilku zdaniach, na czym polega pomysł.' } : {}),
    ...(essence.length > MAX_ESSENCE_LENGTH
      ? { essence: `Opis może mieć najwyżej ${MAX_ESSENCE_LENGTH} znaków.` }
      : {}),
    ...(values.targetGroups.length === 0 ? { targetGroups: 'Wybierz przynajmniej jedną grupę odbiorców.' } : {}),
    ...(values.stage === null ? { stage: 'Wybierz, na jakim etapie jest pomysł.' } : {}),
  }
  if (Object.keys(errors).length > 0 || values.stage === null) return { errors, idea: null }
  return { errors, idea: { title, essence, targetGroups: values.targetGroups, stage: values.stage } }
}
