import { useState } from 'react'
import { Checkbox, Paper, Select, Stack, Text, Textarea, TextInput } from '@mantine/core'
import { ErrorSummary, type FormErrorItem } from '../../components/ErrorSummary'
import { PageHeader } from '../../components/PageHeader'
import { RequireAuth } from '../../auth/RequireAuth'
import { INNOVATION_STAGE_NAMES, TARGET_GROUP_OPTIONS } from '../knowledge/constants'
import { AssistantPanel } from './AssistantPanel'
import { MAX_ESSENCE_LENGTH, MAX_TITLE_LENGTH, validateIdea, type IdeaErrors } from './ideaValidation'

const FIELD_IDS = {
  title: 'idea-title',
  essence: 'idea-essence',
  targetGroups: 'idea-target-groups',
  stage: 'idea-stage',
} as const

const STAGE_OPTIONS = Object.entries(INNOVATION_STAGE_NAMES).map(([value, label]) => ({ value, label }))

function summaryOf(errors: IdeaErrors): FormErrorItem[] {
  return (Object.keys(FIELD_IDS) as (keyof typeof FIELD_IDS)[]).flatMap((field) => {
    const message = errors[field]
    return message ? [{ fieldId: FIELD_IDS[field], message }] : []
  })
}

function AssistantContent() {
  const [title, setTitle] = useState('')
  const [essence, setEssence] = useState('')
  const [targetGroups, setTargetGroups] = useState<string[]>([])
  const [stage, setStage] = useState<string | null>('IDEA')
  const [errors, setErrors] = useState<IdeaErrors>({})

  const resolveIdea = () => {
    const result = validateIdea({ title, essence, targetGroups, stage })
    setErrors(result.errors)
    return result.idea
  }

  return (
    <Stack gap="xl">
      <PageHeader
        title="Asystent kreatora pomysłów"
        subtitle="Opisz pomysł na innowację społeczną, a asystent sprawdzi, czy już coś takiego jest, i podpowie, jak go rozwinąć."
        breadcrumbs={[{ title: 'Strona główna', href: '/' }, { title: 'Asystent pomysłów' }]}
      />

      <ErrorSummary errors={summaryOf(errors)} />

      <Paper withBorder p={{ base: 'md', sm: 'xl' }} radius="lg" component="form" noValidate onSubmit={(e) => e.preventDefault()}>
        <Stack gap="lg">
          <Text size="md">
            Nie wpisuj imion, adresów ani numerów telefonu. Asystent ich nie potrzebuje, a pomysł nie jest zapisywany.
          </Text>

          <TextInput
            id={FIELD_IDS.title}
            label="Tytuł pomysłu"
            description="Krótko, np. „Dowóz seniorów do lekarza”."
            value={title}
            onChange={(e) => setTitle(e.currentTarget.value)}
            error={errors.title}
            maxLength={MAX_TITLE_LENGTH + 50}
            required
            size="lg"
          />

          <Textarea
            id={FIELD_IDS.essence}
            label="Na czym polega pomysł?"
            description="Jaki problem rozwiązuje i jak miałby działać."
            value={essence}
            onChange={(e) => setEssence(e.currentTarget.value)}
            error={errors.essence}
            minRows={5}
            autosize
            required
            size="lg"
          />
          <Text size="sm" c="dimmed" aria-live="off">
            {essence.trim().length} z {MAX_ESSENCE_LENGTH} znaków
          </Text>

          <Checkbox.Group
            label="Dla kogo jest ten pomysł?"
            value={targetGroups}
            onChange={setTargetGroups}
            error={errors.targetGroups}
            required
            size="md"
          >
            <Stack gap="xs" mt="xs">
              {TARGET_GROUP_OPTIONS.map((option, index) => (
                <Checkbox
                  key={option.value}
                  id={index === 0 ? FIELD_IDS.targetGroups : undefined}
                  value={option.value}
                  label={option.label}
                />
              ))}
            </Stack>
          </Checkbox.Group>

          <Select
            id={FIELD_IDS.stage}
            label="Na jakim etapie jest pomysł?"
            data={STAGE_OPTIONS}
            value={stage}
            onChange={setStage}
            error={errors.stage}
            allowDeselect={false}
            required
            size="lg"
          />
        </Stack>
      </Paper>

      <AssistantPanel resolveIdea={resolveIdea} />
    </Stack>
  )
}

export function AssistantPage() {
  return (
    <RequireAuth>
      <AssistantContent />
    </RequireAuth>
  )
}
