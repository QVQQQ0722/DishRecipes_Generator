class ImportFailure(Exception):
    """A safe, user-visible failure; never include credentials or raw HTTP bodies."""

    def __init__(self, code: str, message: str, status: int = 422):
        super().__init__(message)
        self.code = code
        self.message = message
        self.status = status
