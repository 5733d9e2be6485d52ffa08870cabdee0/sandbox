# Filters

When a new `Processor` is requested using the endpoint `/api/v1/bridges/{id}/processors` it is possible to specify one or more `Filters` to apply to `Events` sent to your Bridge instance.
If the `Filter` you provide matches an `Event` then the [Transformation](TRANSFORMATIONS.md) and [Action](ACTIONS.md) of the associated `Processor` are invoked. 

If you do not specify a `Filter` definition for your `Processor`, then your `Processor` will match all `Events` sent to your Bridge Instance.

If there are multiple `Filters` defined for a `Processor`, then all `Filters` must match the `Event` for the [Transformation](TRANSFORMATIONS.md) and [Action](ACTIONS.md) to be invoked.  

## Properties of a Filter

Every `Filter` has 3 properties to specify: 

- `type`: the type of the `Filter`. This must be one of the supported `Filter` types listed below
  - Attempting to use an unknown `Filter` type will result in an Error from the Bridge API.
- `key`: The field in the `Event` that you want to filter on. 
  - This must be a single field only. Arrays of fields to match on are not yet supported. 
- `value(s)`: The value or values to compare to the field identified by the key.

All Events sent to the Bridge must be in CloudEvent format. 
You can use the `key` property of your `Filter` to access Attributes of the `CloudEvent` e.g `id`, `source`, `type`, `version`,
as well as custom attributes you have defined. 
It is also possible to access `CloudEvent` data (like `data.key1`) which is accessed using the dot notation to navigate the `Event` structure.

## Supported Filter Types

The available `Filter` types are: 

### StringEquals

The `StringEquals` evaluates to `true` if the **key** value is equals to the specified `Filter` **value**. 

Assuming that the Filter is the following 

```json

{
  "filters": [
    {
      "type": "StringEquals", 
      "key": "data.name",
      "value": "Jacopo"
    }
  ]
}
```

Then an event like 
```json
{
  ...
  "data": {
    "name": "Jacopo"
  }
}
```

Would evaluate the `Filter` to `true`.

### StringContains

The `StringContains` evaluates to `true` if the **key** value contains any of the values specified in the `Filter` **values**.

Assuming that the `Filter` is the following

```json

{
  "filters": [
    {
      "type": "StringContains", 
      "key": "data.name",
      "values": ["opo", "Marco"]
    }
  ]
}
```

Then an event like
```json
{
  ...
  "data": {
    "name": "Jacopo"
  }
}
```

Would evaluate the `Filter` to `true`.

### StringBeginsWith

The `StringBeginsWith` evaluates to `true` if the **key** value starts with any of the values specified in the `Filter` **values**.

Assuming that the `Filter` is the following

```json

{
  "filters": [
    {
      "type": "StringBeginsWith", 
      "key": "data.name",
      "values": ["Jac", "Mar"]
    }
  ]
}
```

Then an event like
```json
{
  ...
  "data": {
    "name": "Jacopo"
  }
}
```

Would evaluate the `Filter` to `true`.

### ValuesIn

The `ValuesIn` evaluates to `true` if the **key** value is equal to any of the values specified in the `Filter` **values**.

Assuming that the `Filter` is the following

```json

{
  "filters": [
    {
      "type": "ValuesIn", 
      "key": "data.any",
      "values": ["Jac", 2]
    }
  ]
}
```

Then an event like
```json
{
  ...
  "data": {
    "any": 2
  }
}
```

Would evaluate the `Filter` to `true`.


## Combining filters

When a `Filter` array contain more than one entry, the entries are ANDed, meaning all entries must match for the `Filter` to be true.
Assume a `Filter` of:

```json

{
  "filters": [
    {
      "type": "ValuesIn",
      "key": "data.any",
      "values": ["Jac", 2]
    },
    {
      "type": "StringEquals",
      "key": "type",
      "values": "myType" 
    }
  ]
}
```

In this case a CloudEvent like:


```json
{
  ...
  "type": "myType",
  "data": {
    "any": 2
  }
}
```

would make the `Filter` evaluate to `true`.
