package io.crnk.core.queryspec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.crnk.core.engine.document.Resource;
import io.crnk.core.engine.information.resource.ResourceInformation;
import io.crnk.core.engine.internal.utils.CompareUtils;
import io.crnk.core.engine.internal.utils.PreconditionUtil;
import io.crnk.core.exception.BadRequestException;
import io.crnk.core.queryspec.pagingspec.OffsetLimitPagingSpec;
import io.crnk.core.queryspec.pagingspec.PagingSpec;
import io.crnk.core.resource.list.DefaultResourceList;
import io.crnk.core.resource.list.ResourceList;
import io.crnk.core.resource.meta.DefaultPagedMetaInformation;

public class QuerySpec {

	private Class<?> resourceClass;

	private String resourceType;

	private List<FilterSpec> filters = new ArrayList<>();

	private List<SortSpec> sort = new ArrayList<>();

	private List<IncludeFieldSpec> includedFields = new ArrayList<>();

	private List<IncludeRelationSpec> includedRelations = new ArrayList<>();

	private Map<Object, QuerySpec> relatedSpecs = new HashMap<>();

	private PagingSpec pagingSpec;

	public QuerySpec(Class<?> resourceClass) {
		this(resourceClass, null);
	}

	public QuerySpec(String resourceType) {
		this(null, resourceType);
	}

	public QuerySpec(Class<?> resourceClass, String resourceType) {
		verifyNotNull(resourceClass, resourceType);
		if (resourceClass != Resource.class) {
			this.resourceClass = resourceClass;
		}
		this.resourceType = resourceType;
		this.pagingSpec = new OffsetLimitPagingSpec();
	}

	public QuerySpec(ResourceInformation resourceInformation) {
		this(resourceInformation.getResourceClass(), resourceInformation.getResourceType());
	}

	public String getResourceType() {
		return resourceType;
	}

	public Class<?> getResourceClass() {
		return resourceClass;
	}

	/**
	 * Evaluates this querySpec against the provided list in memory. It supports
	 * sorting, filter and paging.
	 * <p>
	 * TODO currently ignores relations and inclusions, has room for
	 * improvements
	 *
	 * @param <T> the type of resources in this Iterable
	 * @param resources resources
	 * @return sorted, filtered list.
	 */
	public <T> DefaultResourceList<T> apply(Iterable<T> resources) {
		DefaultResourceList<T> resultList = new DefaultResourceList<>();
		resultList.setMeta(new DefaultPagedMetaInformation());
		apply(resources, resultList);
		return resultList;
	}

	/**
	 * Evaluates this querySpec against the provided list in memory. It supports
	 * sorting, filter and paging. Make sure that the resultList carries meta
	 * and links information of type PagedMetaInformation resp.
	 * PagedLinksInformation to let Crnk compute pagination links.
	 * <p>
	 * TODO currently ignores relations and inclusions
	 *
	 * @param <T> resource type
	 * @param resources to apply the querySpec to
	 * @param resultList used to return the result (including paging meta information)
	 */
	public <T> void apply(Iterable<T> resources, ResourceList<T> resultList) {
		InMemoryEvaluator eval = new InMemoryEvaluator();
		eval.eval(resources, this, resultList);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((filters == null) ? 0 : filters.hashCode());
		result = prime * result + ((includedFields == null) ? 0 : includedFields.hashCode());
		result = prime * result + ((includedRelations == null) ? 0 : includedRelations.hashCode());
		result = prime * result + ((pagingSpec == null) ? 0 : pagingSpec.hashCode());
		result = prime * result + ((relatedSpecs == null) ? 0 : relatedSpecs.hashCode());
		result = prime * result + ((sort == null) ? 0 : sort.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		QuerySpec other = (QuerySpec) obj;
		return CompareUtils.isEquals(filters, other.filters) // NOSONAR
				&& CompareUtils.isEquals(includedFields, other.includedFields)
				&& CompareUtils.isEquals(includedRelations, other.includedRelations)
				&& CompareUtils.isEquals(pagingSpec, other.pagingSpec)
				&& CompareUtils.isEquals(relatedSpecs, other.relatedSpecs)
				&& CompareUtils.isEquals(sort, other.sort);
	}

	public Long getLimit() {
		if (pagingSpec instanceof OffsetLimitPagingSpec) {
			return ((OffsetLimitPagingSpec) pagingSpec).getLimit();
		}

		throw new UnsupportedOperationException("Not instance of OffsetLimitPagingSpec");
	}

	public void setLimit(Long limit) {
		if (pagingSpec instanceof OffsetLimitPagingSpec) {
			((OffsetLimitPagingSpec) pagingSpec).setLimit(limit);
		}
		else {
			throw new UnsupportedOperationException("Not instance of  OffsetLimitPagingSpec");
		}
	}

	public long getOffset() {
		if (pagingSpec instanceof OffsetLimitPagingSpec) {
			return ((OffsetLimitPagingSpec) pagingSpec).getOffset();
		}

		throw new UnsupportedOperationException("Not instance of OffsetLimitPagingSpec");
	}

	public void setOffset(long offset) {
		if (pagingSpec instanceof OffsetLimitPagingSpec) {
			((OffsetLimitPagingSpec) pagingSpec).setOffset(offset);
		}
		else {
			throw new UnsupportedOperationException("Not instance of OffsetLimitPagingSpec");
		}
	}

	public PagingSpec getPagingSpec() {
		return pagingSpec;
	}

	public QuerySpec setPagingSpec(final PagingSpec pagingSpec) {
		this.pagingSpec = pagingSpec;
		return this;
	}

	public List<FilterSpec> getFilters() {
		return filters;
	}

	public void setFilters(List<FilterSpec> filters) {
		this.filters = filters;
	}

	public FilterSpec getFilter(final String name) {
		for (FilterSpec filterSpec : filters) {
			if (filterSpec.getAttributePath().contains(name)) {
				return filterSpec;
			}
		}

		return null;
	}

	public FilterSpec getFilterOrThrow(final String name) {
		FilterSpec filter = getFilter(name);
		if (filter == null) {
			throw new BadRequestException(String.format("Filter '%s' not found", name));
		}

		return filter;
	}

	public List<SortSpec> getSort() {
		return sort;
	}

	public void setSort(List<SortSpec> sort) {
		this.sort = sort;
	}

	public List<IncludeFieldSpec> getIncludedFields() {
		return includedFields;
	}

	public void setIncludedFields(List<IncludeFieldSpec> includedFields) {
		this.includedFields = includedFields;
	}

	public List<IncludeRelationSpec> getIncludedRelations() {
		return includedRelations;
	}

	public void setIncludedRelations(List<IncludeRelationSpec> includedRelations) {
		this.includedRelations = includedRelations;
	}

	/**
	 * @deprecated make use of getNestedSpecs
	 */
	@Deprecated
	public Map<Class<?>, QuerySpec> getRelatedSpecs() {
		return (Map) relatedSpecs;
	}

	public Collection<QuerySpec> getNestedSpecs() {
		return Collections.unmodifiableCollection(relatedSpecs.values());
	}

	public void setNestedSpecs(Collection<QuerySpec> specs) {
		this.relatedSpecs.clear();
		for (QuerySpec spec : specs) {
			if (spec.getResourceClass() != null) {
				relatedSpecs.put(spec.getResourceClass(), spec);
			}
			else {
				relatedSpecs.put(spec.getResourceType(), spec);
			}
		}
	}

	@Deprecated
	public void setRelatedSpecs(Map<Class<?>, QuerySpec> relatedSpecs) {
		this.relatedSpecs = (Map) relatedSpecs;
	}

	public void addFilter(FilterSpec filterSpec) {
		this.filters.add(filterSpec);
	}

	public void addSort(SortSpec sortSpec) {
		this.sort.add(sortSpec);
	}

	public void includeField(List<String> attrPath) {
		this.includedFields.add(new IncludeFieldSpec(attrPath));
	}

	public void includeRelation(List<String> attrPath) {
		this.includedRelations.add(new IncludeRelationSpec(attrPath));
	}

	public QuerySpec getQuerySpec(String resourceType) {
		if (resourceType.equals(this.resourceType)) {
			return this;
		}
		return relatedSpecs.get(resourceType);
	}

	/**
	 * @param resourceClass resource class
	 * @return QuerySpec for the given class, either the root QuerySpec or any
	 * related QuerySpec.
	 */
	public QuerySpec getQuerySpec(Class<?> resourceClass) {
		if (resourceClass.equals(this.resourceClass)) {
			return this;
		}
		return relatedSpecs.get(resourceClass);
	}


	public QuerySpec getQuerySpec(ResourceInformation resourceInformation) {
		QuerySpec querySpec = getQuerySpec(resourceInformation.getResourceType());
		if (querySpec == null) {
			querySpec = getQuerySpec(resourceInformation.getResourceClass());
		}
		return querySpec;
	}

	public QuerySpec getOrCreateQuerySpec(String resourceType) {
		return getOrCreateQuerySpec(null, resourceType);
	}

	public QuerySpec getOrCreateQuerySpec(ResourceInformation resourceInformation) {
		return getOrCreateQuerySpec(resourceInformation.getResourceClass(), resourceInformation.getResourceType());
	}

	public QuerySpec getOrCreateQuerySpec(Class<?> targetResourceClass) {
		return getOrCreateQuerySpec(targetResourceClass, null);
	}

	public QuerySpec getOrCreateQuerySpec(Class<?> targetResourceClass, String targetResourceType) {
		verifyNotNull(targetResourceClass, targetResourceType);

		QuerySpec querySpec = null;
		if (querySpec == null && targetResourceType != null) {
			querySpec = getQuerySpec(targetResourceType);
		}
		if (targetResourceClass != null) {
			querySpec = getQuerySpec(targetResourceClass);
		}
		if (querySpec == null) {
			querySpec = new QuerySpec(targetResourceClass, targetResourceType);
			if (targetResourceClass != null) {
				relatedSpecs.put(targetResourceClass, querySpec);
			}
			if (targetResourceType != null) {
				relatedSpecs.put(targetResourceType, querySpec);
			}
		}
		querySpec.setPagingSpec(pagingSpec);
		return querySpec;
	}

	private static void verifyNotNull(Class<?> targetResourceClass, String targetResourceType) {
		PreconditionUtil.assertNotNull("at least one parameter must not be null",
				targetResourceClass == null && targetResourceType == null);
		if (targetResourceClass == Resource.class && targetResourceType == null) {
			throw new IllegalArgumentException("must specify resourceType if io.crnk.core.engine.document.Resource is used");
		}
	}


	public void putRelatedSpec(Class<?> relatedResourceClass, QuerySpec relatedSpec) {
		if (relatedResourceClass.equals(resourceClass)) {
			throw new IllegalArgumentException("cannot set related spec with root resourceClass");
		}
		relatedSpecs.put(relatedResourceClass, relatedSpec);
	}

	/**
	 * @return use clone() instead
	 */
	@Deprecated
	public QuerySpec duplicate() {
		return clone();
	}

	public QuerySpec clone() {
		QuerySpec copy = new QuerySpec(resourceClass);
		if (pagingSpec != null) {
			copy.pagingSpec = pagingSpec.clone();
		}
		copy.includedFields.addAll(includedFields);
		copy.includedRelations.addAll(includedRelations);
		for (SortSpec sortSpec : sort) {
			copy.sort.add(sortSpec.clone());
		}
		for (FilterSpec filterSpec : filters) {
			copy.filters.add(filterSpec.clone());
		}
		Iterator<Entry<Object, QuerySpec>> iterator = relatedSpecs.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<Object, QuerySpec> entry = iterator.next();
			copy.relatedSpecs.put(entry.getKey(), entry.getValue().duplicate());
		}
		return copy;
	}


	@Override
	public String toString() {
		return "QuerySpec{" +
				(resourceClass != null ? "resourceClass=" + resourceClass.getName() : "") +
				(resourceType != null ? "resourceType=" + resourceType : "") +
				(pagingSpec != null ? ", paging=" + pagingSpec : "") +
				(!filters.isEmpty() ? ", filters=" + filters : "") +
				(!sort.isEmpty() ? ", sort=" + sort : "") +
				(!includedFields.isEmpty() ? ", includedFields=" + includedFields : "") +
				(!includedRelations.isEmpty() ? ", includedRelations=" + includedRelations : "") +
				(!relatedSpecs.isEmpty() ? ", relatedSpecs=" + relatedSpecs : "") +
				'}';
	}
}
