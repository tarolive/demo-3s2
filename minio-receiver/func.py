from parliament import Context, event


@event
def main(context: Context):

    print(context)

    return context.cloud_event.data
