# Filters

When a new `Processor` is requested using the endpoint `/api/v1/bridges/{id}/processors` it is possible to specify one or more filters to apply to the ingress event. If there is a match, then the `action` of the processor is called. 

Every filter has 3 properties to specify: 

- `type`: the type of the filter. This can be `StringEquals`, `StringBeginsWith`, `StringContains`.
- `key`: The field in the event data that you're using for filtering. It can be a number, boolean, string. **Arrays are not supported yet**.
- `values`: The value or values to compare to the key.

For events in **Cloud Events schema**, you can use the values for the key: `eventid`, `source`, `eventtype`, `eventtypeversion`, or event data (like `data.key1`). You can navigate the object with the `.` (dot) notation.

## Filter types

The available filter types are: 

### StringEquals

The `StringEquals` evaluates to `true` if the **key** value is equals to the specified filter **value**. 

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

Would evaluate the Filter to `true`.

### StringContains

The `StringContains` evaluates to `true` if the **key** value contains any of the values specified in the filter **values**.

Assuming that the Filter is the following

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

Would evaluate the Filter to `true`.

### StringBeginsWith

The `StringBeginsWith` evaluates to `true` if the **key** value starts with any of the values specified in the filter **values**.

Assuming that the Filter is the following

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

Would evaluate the Filter to `true`.

### ValuesIn

The `ValuesIn` evaluates to `true` if the **key** value is equal to any of the values specified in the filter **values**.

Assuming that the Filter is the following

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

Would evaluate the Filter to `true`.


