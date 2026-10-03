export const SOCIAL_AREA_OPTIONS = [
  { value: 'AGING', label: 'Wsparcie seniorów i osób starszych' },
  { value: 'MENTAL_HEALTH', label: 'Zdrowie psychiczne i samopoczucie' },
  { value: 'LONELINESS', label: 'Przeciwdziałanie samotności i integracja' },
  { value: 'DIGITAL_EXCLUSION', label: 'Pomoc w korzystaniu z internetu i technologii' },
  { value: 'SERVICE_ACCESS', label: 'Łatwiejszy dojazd do lekarza i urzędu' },
  { value: 'COORDINATION', label: 'Współpraca lokalna i pomoc sąsiedzka' },
  { value: 'DEPOPULATION', label: 'Wsparcie małych miejscowości i wsi' },
  { value: 'OTHER', label: 'Inne potrzeby społeczne' },
] as const

export const SOCIAL_AREA_NAMES: Record<string, string> = {
  AGING: 'Wsparcie seniorów i osób starszych',
  MENTAL_HEALTH: 'Zdrowie psychiczne i samopoczucie',
  LONELINESS: 'Przeciwdziałanie samotności i integracja' ,
  DIGITAL_EXCLUSION: 'Pomoc w korzystaniu z internetu i technologii',
  SERVICE_ACCESS: 'Łatwiejszy dojazd do lekarza i urzędu',
  COORDINATION: 'Współpraca lokalna i pomoc sąsiedzka',
  DEPOPULATION: 'Wsparcie małych miejscowości i wsi',
  OTHER: 'Inne potrzeby społeczne',
}

export const TARGET_GROUP_OPTIONS = [
  { value: 'SENIORS', label: 'Seniorzy i osoby starsze' },
  { value: 'YOUTH', label: 'Dzieci i młodzież' },
  { value: 'PEOPLE_WITH_DISABILITIES', label: 'Osoby z niepełnosprawnościami' },
  { value: 'FAMILIES', label: 'Rodziny i opiekunowie' },
  { value: 'RESIDENTS', label: 'Wszyscy mieszkańcy' },
  { value: 'NGOS', label: 'Organizacje pozarządowe (NGO)' },
  { value: 'LOCAL_GOVERNMENTS', label: 'Samorządy i jednostki publiczne' },
] as const

export const TARGET_GROUP_NAMES: Record<string, string> = {
  SENIORS: 'Seniorzy i osoby starsze',
  YOUTH: 'Dzieci i młodzież',
  PEOPLE_WITH_DISABILITIES: 'Osoby z niepełnosprawnościami',
  FAMILIES: 'Rodziny i opiekunowie',
  RESIDENTS: 'Wszyscy mieszkańcy',
  NGOS: 'Organizacje pozarządowe (NGO)',
  LOCAL_GOVERNMENTS: 'Samorządy i jednostki publiczne',
}

export const INNOVATION_STAGE_NAMES: Record<string, string> = {
  IDEA: 'Nowy pomysł (w przygotowaniu)',
  PILOT: 'Sprawdzane w praktyce (pilotaż)',
  TESTED: 'Przetestowane z mieszkańcami',
  IMPLEMENTED: 'Gotowe i działające rozwiązanie',
}

export const MATERIAL_TYPE_OPTIONS = [
  { value: 'GUIDE', label: 'Poradnik krok po kroku' },
  { value: 'REPORT', label: 'Raport i publikacja' },
  { value: 'VIDEO', label: 'Film instruktażowy' },
  { value: 'CANVAS', label: 'Szablon roboczy (Canva)' },
] as const

export const MATERIAL_TYPE_NAMES: Record<string, string> = {
  GUIDE: 'Poradnik krok po kroku',
  REPORT: 'Raport i publikacja',
  VIDEO: 'Film instruktażowy',
  CANVAS: 'Szablon roboczy (Canva)',
}
