import java.io.File;

/* Dear john-from-the-future, this is john-from-the-past. You
almost certainly think I messed up here and that the code could be
cleaned up. Well don't! It’s like this for a reason! */

class MyFile extends File {
	
	MyFile(String filename) {
		super(filename);
	}

	@Override
	public String toString() {
		if(this.getPath().compareTo("/")!=0)
			return this.getName();
		else
			return this.getPath();
	}
}
