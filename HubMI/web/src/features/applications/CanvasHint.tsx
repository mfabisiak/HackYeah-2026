import { Alert, Anchor, List, Text, VisuallyHidden } from '@mantine/core'
import { IconBulb } from '@tabler/icons-react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { hubApi } from '../../api/hubApi'
import { unwrapApiResult } from '../../api/queryClient'

/** Points to ROPS canvas templates, which help to think the idea through before filling in the form. */
export function CanvasHint() {
  const { data } = useQuery({
    queryKey: ['materials', 'canvas'],
    queryFn: () => unwrapApiResult(hubApi.materials.list(undefined, undefined, 'CANVAS', 0, 3)),
    staleTime: 5 * 60_000,
  })
  const canvases = data?.items ?? []
  if (canvases.length === 0) return null

  return (
    <Alert
      variant="light"
      color="blue"
      role="note"
      icon={<IconBulb size={22} aria-hidden="true" />}
      title="Potrzebujesz pomocy? Pobierz canvę"
      aria-label="Pomoc: szablony canva"
    >
      <Text mb="xs">Szablon roboczy (canva) pomoże uporządkować pomysł, zanim wypełnisz formularz.</Text>
      <List spacing={4}>
        {canvases.map((material) => (
          <List.Item key={material.id}>
            <Anchor href={material.url} target="_blank" rel="noopener noreferrer">
              {material.title}
              <VisuallyHidden component="span"> (otwiera się w nowej karcie)</VisuallyHidden>
            </Anchor>
          </List.Item>
        ))}
      </List>
      <Text mt="xs" size="sm">
        <Anchor component={Link} to="/materialy?type=CANVAS">
          Zobacz wszystkie szablony
        </Anchor>
      </Text>
    </Alert>
  )
}
