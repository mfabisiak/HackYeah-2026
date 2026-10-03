import type { ReactNode } from 'react'
import { Anchor, Box, Breadcrumbs, Group, Stack, Text, Title } from '@mantine/core'
import { Link } from 'react-router-dom'

export interface BreadcrumbItem {
  title: string
  href?: string
}

export interface PageHeaderProps {
  title: string
  subtitle?: string
  breadcrumbs?: BreadcrumbItem[]
  actions?: ReactNode
}

export function PageHeader({
  title,
  subtitle,
  breadcrumbs,
  actions,
}: PageHeaderProps) {
  return (
    <Box mb="xl" component="header">
      {breadcrumbs && breadcrumbs.length > 0 && (
        <Box component="nav" aria-label="Ścieżka nawigacyjna" mb="xs">
          <Breadcrumbs separator="/" separatorMargin="xs">
            {breadcrumbs.map((item, index) => {
              const isLast = index === breadcrumbs.length - 1
              if (isLast || !item.href) {
                return (
                  <Text
                    key={item.title}
                    size="sm"
                    c="dimmed"
                    aria-current={isLast ? 'page' : undefined}
                  >
                    {item.title}
                  </Text>
                )
              }
              return (
                <Anchor
                  key={item.title}
                  component={Link}
                  to={item.href}
                  size="sm"
                  underline="hover"
                >
                  {item.title}
                </Anchor>
              )
            })}
          </Breadcrumbs>
        </Box>
      )}

      <Group justify="space-between" align="flex-start" gap="md">
        <Stack gap="xs">
          <Title order={1} size="h2">
            {title}
          </Title>
          {subtitle && (
            <Text c="dimmed" size="md">
              {subtitle}
            </Text>
          )}
        </Stack>

        {actions && <Group gap="sm">{actions}</Group>}
      </Group>
    </Box>
  )
}
