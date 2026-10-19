import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getCategories, getProjects, getProjectDetail } from '@/api/heritage'

export const useHeritageStore = defineStore('heritage', () => {
  const categories = ref([])
  const currentHeritage = ref(null)
  const projectCache = ref(new Map())

  async function loadCategories() {
    if (categories.value.length > 0) return categories.value
    categories.value = await getCategories()
    return categories.value
  }

  async function fetchProjects(params) {
    return getProjects(params)
  }

  async function fetchProjectDetail(id) {
    if (projectCache.value.has(id)) {
      return projectCache.value.get(id)
    }
    const detail = await getProjectDetail(id)
    projectCache.value.set(id, detail)
    return detail
  }

  function setCurrentHeritage(project) {
    currentHeritage.value = project
  }

  function clearCurrentHeritage() {
    currentHeritage.value = null
  }

  return {
    categories,
    currentHeritage,
    loadCategories,
    fetchProjects,
    fetchProjectDetail,
    setCurrentHeritage,
    clearCurrentHeritage,
  }
})